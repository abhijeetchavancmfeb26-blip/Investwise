import { useState } from "react";
import { Link } from "react-router-dom";
import { del, get, toError } from "../lib/api";
import { useFetch } from "../lib/useFetch";
import { useAuth } from "../lib/auth";
import {
  Badge,
  Empty,
  Loading,
  Modal,
  Notice,
  PageTitle,
  Progress,
  Table,
} from "../components/Ui";
import { date, money } from "../lib/format";

export default function Subscription() {
  const { refresh } = useAuth();
  const [cancelling, setCancelling] = useState(false);
  const [notice, setNotice] = useState(null);

  const {
    data: current,
    loading,
    reload,
  } = useFetch(() => get("/api/v1/subscriptions/me"), []);
  const { data: history } = useFetch(
    () => get("/api/v1/subscriptions/history", { size: 10 }),
    [],
  );
  const { data: payments } = useFetch(
    () => get("/api/v1/payments/me", { size: 10 }),
    [],
  );

  const confirmCancel = async () => {
    try {
      await del(`/api/v1/subscriptions/${current.id}`);
      setNotice({
        type: "success",
        text: "Subscription cancelled. Access continues until the end of your paid term.",
      });
      setCancelling(false);
      await refresh();
      reload();
    } catch (err) {
      setNotice({ type: "error", text: toError(err).message });
      setCancelling(false);
    }
  };

  if (loading) return <Loading label="Loading your subscription…" />;

  const termProgress = current
    ? Math.max(
        0,
        Math.min(
          100,
          ((Date.now() - new Date(current.startDate)) /
            (new Date(current.endDate) - new Date(current.startDate))) *
            100,
        ),
      )
    : 0;

  return (
    <div className="space-y-6">
      <PageTitle
        title="Subscription"
        subtitle="Your plan, billing history and payment receipts."
      />

      {notice && (
        <Notice type={notice.type} onDismiss={() => setNotice(null)}>
          {notice.text}
        </Notice>
      )}

      {current ? (
        <div className="card">
          <div className="flex flex-wrap items-start justify-between gap-6">
            <div className="min-w-0 flex-1">
              <div className="flex flex-wrap items-center gap-2">
                <h2 className="text-xl font-bold text-slate-900">
                  {current.plan.name}
                </h2>
                <Badge status={current.status} />
                {current.expiringSoon && (
                  <span className="badge bg-amber-100 text-amber-800">
                    Expires soon
                  </span>
                )}
              </div>
              <p className="mt-2 text-sm text-slate-600">
                {current.plan.description}
              </p>

              <dl className="mt-4 grid gap-4 sm:grid-cols-3 text-sm">
                <div>
                  <dt className="text-xs uppercase text-slate-400">Started</dt>
                  <dd className="mt-0.5 font-semibold">
                    {date(current.startDate)}
                  </dd>
                </div>
                <div>
                  <dt className="text-xs uppercase text-slate-400">Ends</dt>
                  <dd className="mt-0.5 font-semibold">
                    {date(current.endDate)}
                  </dd>
                </div>
                <div>
                  <dt className="text-xs uppercase text-slate-400">
                    Remaining
                  </dt>
                  <dd className="mt-0.5 font-semibold">
                    {current.daysRemaining} days
                  </dd>
                </div>
              </dl>

              <div className="mt-4">
                <Progress value={termProgress} />
              </div>
            </div>

            <div className="shrink-0 space-y-2">
              <Link to="/analytics" className="btn-primary w-full">
                Open analytics
              </Link>
              {current.status === "ACTIVE" && (
                <button
                  type="button"
                  onClick={() => setCancelling(true)}
                  className="btn-secondary w-full"
                >
                  Cancel subscription
                </button>
              )}
            </div>
          </div>

          <div className="mt-5 border-t border-slate-200 pt-4">
            <p className="mb-2 text-xs font-bold uppercase text-slate-500">
              Included in your plan
            </p>
            <ul className="grid gap-1.5 sm:grid-cols-2">
              {current.plan.features.map((feature) => (
                <li key={feature} className="text-sm text-slate-600">
                  ✓ {feature}
                </li>
              ))}
            </ul>
          </div>
        </div>
      ) : (
        <Empty
          title="You are on the free Starter plan"
          description="Premium adds advanced analytics, diversification scoring, a rebalancing plan,
                            tax classification, unlimited goals and premium reports."
          action={
            <Link to="/pricing" className="btn-primary">
              Compare plans
            </Link>
          }
        />
      )}

      {/* ---------- payments ---------- */}
      <div>
        <h2 className="mb-3 font-semibold text-slate-900">Payment history</h2>
        {!payments?.content?.length ? (
          <p className="card text-center text-sm text-slate-400">
            No payments recorded yet.
          </p>
        ) : (
          <Table
            headers={["Date", "Plan", "Invoice", "Method", "Amount", "Status"]}
          >
            {payments.content.map((payment) => (
              <tr key={payment.id}>
                <td>{date(payment.createdAt, true)}</td>
                <td className="font-medium text-slate-800">
                  {payment.planName ?? "—"}
                </td>
                <td className="font-mono text-xs text-slate-500">
                  {payment.invoiceNo ?? "—"}
                </td>
                <td className="text-xs uppercase">{payment.method ?? "—"}</td>
                <td className="font-semibold">{money(payment.amount)}</td>
                <td>
                  <Badge status={payment.status} />
                  {payment.failureReason && (
                    <p className="mt-0.5 text-xs text-red-600">
                      {payment.failureReason}
                    </p>
                  )}
                </td>
              </tr>
            ))}
          </Table>
        )}
      </div>

      {/* ---------- subscription history ---------- */}
      {history?.content?.length > 0 && (
        <div>
          <h2 className="mb-3 font-semibold text-slate-900">
            Subscription history
          </h2>
          <Table
            headers={["Plan", "Tier", "Started", "Ended", "Price", "Status"]}
          >
            {history.content.map((subscription) => (
              <tr key={subscription.id}>
                <td className="font-medium text-slate-800">
                  {subscription.plan.name}
                </td>
                <td className="text-xs uppercase">{subscription.plan.tier}</td>
                <td>{date(subscription.startDate)}</td>
                <td>{date(subscription.endDate)}</td>
                <td className="font-semibold">
                  {money(subscription.plan.price)}
                </td>
                <td>
                  <Badge status={subscription.status} />
                </td>
              </tr>
            ))}
          </Table>
        </div>
      )}

      <Modal
        open={cancelling}
        onClose={() => setCancelling(false)}
        title="Cancel your subscription?"
        footer={
          <>
            <button
              type="button"
              onClick={() => setCancelling(false)}
              className="btn-secondary"
            >
              Keep my plan
            </button>
            <button
              type="button"
              onClick={confirmCancel}
              className="btn-danger"
            >
              Cancel subscription
            </button>
          </>
        }
      >
        <ul className="space-y-2 text-sm leading-relaxed text-slate-700">
          <li>
            ✓ You keep premium access until{" "}
            <strong>{date(current?.endDate)}</strong>, the end of the term you
            have already paid for.
          </li>
          <li>
            ✓ Nothing is deleted. Goals beyond the free limit become read-only
            rather than disappearing.
          </li>
          <li>
            ✕ We do not pro-rate refunds for the unused portion of a term.
          </li>
        </ul>
      </Modal>
    </div>
  );
}
