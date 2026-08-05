package com.investwise.investment.service;

import com.investwise.investment.common.ApiException;
import com.investwise.investment.dto.Responses;
import com.investwise.investment.model.Enums;
import com.investwise.investment.security.AuthUser;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * PDF and CSV report generation.
 * <p>
 * Collapsed from roughly 500 lines to a generic table renderer plus one method per
 * report that supplies a title, headers and rows. The original repeated the same
 * cell-styling code six times.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm");
    private static final Color BRAND = new Color(15, 118, 110);

    private final PortfolioService portfolioService;
    private final GoalService goalService;
    private final RecommendationService recommendationService;
    private final SubscriptionService subscriptions;
    private final ActivityService activity;

    /** Title, column headers and rows — everything a report actually differs by. */
    private record Table(String title, List<String> headers, List<List<String>> rows) { }

    // ------------------------------------------------------------------

    public byte[] pdf(AuthUser user, Enums.ReportType type) {
        assertEntitled(user, type);
        Table table = build(user, type);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 48, 48);
            PdfWriter.getInstance(document, out);
            document.open();

            document.add(heading("InvestWise", 22, BRAND));
            document.add(heading(type.label(), 15, Color.BLACK));
            document.add(small("Prepared for %s  |  %s".formatted(user.name(), LocalDateTime.now().format(STAMP))));

            if (table.rows().isEmpty()) {
                document.add(small("No data to report yet."));
            } else {
                document.add(table(table));
            }

            document.add(small("\nGenerated from data you have entered, for information only. Not investment "
                    + "advice. Projected values assume the stated rate of return and are not guaranteed. "
                    + "Investments in securities are subject to market risk."));
            document.close();

            activity.record(user.id(), user.email(), "REPORT_DOWNLOADED", type.label() + " (PDF)");
            return out.toByteArray();

        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to render the PDF report", ex);
        }
    }

    public byte[] csv(AuthUser user, Enums.ReportType type) {
        assertEntitled(user, type);
        Table table = build(user, type);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                     .setHeader(table.headers().toArray(String[]::new)).build())) {

            for (List<String> row : table.rows()) {
                printer.printRecord(row);
            }
            printer.flush();

            activity.record(user.id(), user.email(), "REPORT_DOWNLOADED", type.label() + " (CSV)");
            return out.toByteArray();

        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to render the CSV report", ex);
        }
    }

    public String filename(Enums.ReportType type, String extension) {
        return "investwise-%s-%s.%s".formatted(type.name().toLowerCase().replace('_', '-'),
                LocalDateTime.now().format(FILE_STAMP), extension);
    }

    // ------------------------------------------------------------------
    //  One method per report; everything else is shared
    // ------------------------------------------------------------------

    private Table build(AuthUser user, Enums.ReportType type) {
        return switch (type) {
            case PORTFOLIO_SUMMARY, PREMIUM_ANALYTICS -> holdings(user);
            case GOAL_PROGRESS -> goals(user);
            case TRANSACTION_STATEMENT -> transactions(user);
            case RECOMMENDATION_SHEET -> recommendations(user);
            case TAX_STATEMENT -> tax(user);
        };
    }

    private Table holdings(AuthUser user) {
        Responses.PortfolioView portfolio = portfolioService.get(user.id());
        return new Table("Holdings",
                List.of("Product", "Category", "Units", "Invested", "Current", "Gain", "Return %"),
                portfolio.holdings().stream()
                        .map(h -> List.of(h.productName(), h.category(), num(h.units()),
                                money(h.investedAmount()), money(h.currentValue()),
                                money(h.gain()), pct(h.gainPct())))
                        .toList());
    }

    private Table goals(AuthUser user) {
        return new Table("Goal progress",
                List.of("Goal", "Type", "Target", "Saved", "Progress %", "Required / month", "Target date", "Status"),
                goalService.listAll(user.id()).stream()
                        .map(g -> List.of(g.title(), g.goalTypeLabel(), money(g.targetAmount()),
                                money(g.currentAmount()), pct(g.progressPct()),
                                money(g.requiredMonthly()), String.valueOf(g.targetDate()),
                                String.valueOf(g.status())))
                        .toList());
    }

    private Table transactions(AuthUser user) {
        return new Table("Transactions",
                List.of("Reference", "Date", "Product", "Type", "Units", "Price", "Amount"),
                portfolioService.transactions(user.id(), null, null, null, 0, 500).content().stream()
                        .map(t -> List.of(t.referenceNo(), t.createdAt().toLocalDate().toString(),
                                nullSafe(t.productName()), String.valueOf(t.type()), num(t.units()),
                                money(t.price()), money(t.amount())))
                        .toList());
    }

    private Table recommendations(AuthUser user) {
        Responses.RecommendationView view;
        try {
            view = recommendationService.latest(user.id(), null);
        } catch (RuntimeException ex) {
            throw ApiException.badRequest("Generate a set of recommendations before exporting them");
        }
        return new Table("Suggested allocation",
                List.of("Product", "Category", "Risk", "Allocation %", "Amount", "Match", "Rationale"),
                view.items().stream()
                        .map(i -> List.of(i.productName(), i.category(), i.riskLevel(),
                                pct(i.allocationPct()), money(i.amount()), pct(i.matchScore()),
                                nullSafe(i.rationale())))
                        .toList());
    }

    private Table tax(AuthUser user) {
        Responses.PortfolioView portfolio = portfolioService.get(user.id());
        return new Table("Unrealised gains by holding period",
                List.of("Product", "Purchased", "Days held", "Invested", "Current", "Unrealised gain", "Classification"),
                portfolio.holdings().stream()
                        .map(h -> List.of(h.productName(), String.valueOf(h.purchaseDate()),
                                String.valueOf(h.holdingDays()), money(h.investedAmount()),
                                money(h.currentValue()), money(h.gain()),
                                h.longTerm() ? "Long term" : "Short term"))
                        .toList());
    }

    private void assertEntitled(AuthUser user, Enums.ReportType type) {
        if (type.premiumOnly() && !subscriptions.tierOf(user.id()).isPremium()) {
            throw ApiException.forbidden(
                    "%s is a Premium report. Upgrade your plan to download it.".formatted(type.label()));
        }
    }

    // ---------------- PDF rendering, written once ----------------

    private Paragraph heading(String text, float size, Color colour) {
        Paragraph paragraph = new Paragraph(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, size, colour));
        paragraph.setSpacingAfter(6);
        return paragraph;
    }

    private Paragraph small(String text) {
        Paragraph paragraph = new Paragraph(text, FontFactory.getFont(FontFactory.HELVETICA, 8.5f, Color.GRAY));
        paragraph.setSpacingAfter(14);
        return paragraph;
    }

    private PdfPTable table(Table source) {
        PdfPTable table = new PdfPTable(source.headers().size());
        table.setWidthPercentage(100);

        source.headers().forEach(header -> {
            PdfPCell cell = new PdfPCell(new Phrase(header,
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, Color.WHITE)));
            cell.setBackgroundColor(BRAND);
            cell.setPadding(6);
            cell.setBorderColor(BRAND);
            table.addCell(cell);
        });

        int index = 0;
        for (List<String> row : source.rows()) {
            Color background = (index++ % 2 == 0) ? Color.WHITE : new Color(248, 250, 252);
            for (String value : row) {
                PdfPCell cell = new PdfPCell(new Phrase(nullSafe(value),
                        FontFactory.getFont(FontFactory.HELVETICA, 8, Color.DARK_GRAY)));
                cell.setBackgroundColor(background);
                cell.setPadding(5);
                cell.setBorderColor(new Color(226, 232, 240));
                cell.setHorizontalAlignment(Element.ALIGN_LEFT);
                table.addCell(cell);
            }
        }
        return table;
    }

    private String money(BigDecimal value) {
        return value == null ? "-" : "INR " + Money.round(value).toPlainString();
    }

    private String pct(BigDecimal value) {
        return value == null ? "-" : Money.round(value).toPlainString() + "%";
    }

    private String num(BigDecimal value) {
        return value == null ? "-" : Money.round(value).toPlainString();
    }

    private String nullSafe(String value) {
        return value == null ? "-" : value;
    }
}
