-- =====================================================================
--  InvestWise-Lite :: seed data
--  admin@investwise.in / Admin@123
--  rahul.sharma@example.com / User@123   (free)
--  priya.nair@example.com   / User@123   (premium)
-- =====================================================================

USE investwise_user;

INSERT INTO roles (name) VALUES ('ROLE_USER'), ('ROLE_ADMIN')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO users
 (first_name,last_name,email,password,phone,date_of_birth,gender,pan_number,annual_income,
  occupation,city,state,pincode,email_verified,status,tier,created_at,updated_at)
VALUES
 ('Aarav','Mehta','admin@investwise.in','$2a$10$p946U5qXCOJ.uu2C.nJpbe6owu5xCy78tE0UJ8QczXwFvgpTFBN2q',
  '9876500001','1988-04-12','MALE','ABCPE1234F',2500000,'Platform Administrator','Pune','Maharashtra','411001',
  TRUE,'ACTIVE','PREMIUM',NOW(),NOW()),
 ('Rahul','Sharma','rahul.sharma@example.com','$2a$10$wXVYf8Usb1aH3B8xyks4yeBXQZpYyI/kkh6RY/0Lrkl/Yt4fRsCyy',
  '9876500002','1995-09-23','MALE','BCDPA2345G',900000,'Software Engineer','Bengaluru','Karnataka','560001',
  TRUE,'ACTIVE','FREE',NOW(),NOW()),
 ('Priya','Nair','priya.nair@example.com','$2a$10$wXVYf8Usb1aH3B8xyks4yeBXQZpYyI/kkh6RY/0Lrkl/Yt4fRsCyy',
  '9876500003','1992-01-08','FEMALE','CDEPB3456H',1800000,'Product Manager','Mumbai','Maharashtra','400001',
  TRUE,'ACTIVE','PREMIUM',NOW(),NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u JOIN roles r
 ON  (u.email = 'admin@investwise.in' AND r.name IN ('ROLE_ADMIN','ROLE_USER'))
  OR (u.email IN ('rahul.sharma@example.com','priya.nair@example.com') AND r.name = 'ROLE_USER')
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id);

-- ---------------------------------------------------------------------
USE investwise_investment;

INSERT INTO products
 (code,name,description,category,risk_level,expected_return,min_investment,lock_in_months,
  fund_house,expense_ratio,rating,premium_only,active,created_at,updated_at)
VALUES
 ('IW-EQ-001','Bluechip Large Cap Fund','Diversified large-cap equity fund tracking India''s top 100 companies. Suited for long-horizon wealth creation.','EQUITY','MODERATE',13.50,5000,0,'Axis AMC',1.05,4,FALSE,TRUE,NOW(),NOW()),
 ('IW-EQ-002','Emerging Mid Cap Fund','Concentrated mid-cap portfolio targeting scalable businesses. Higher volatility, higher upside.','EQUITY','HIGH',16.80,5000,0,'HDFC AMC',1.42,4,FALSE,TRUE,NOW(),NOW()),
 ('IW-EQ-003','Small Cap Alpha Fund','Aggressive small-cap fund for a 7+ year horizon and high loss tolerance.','EQUITY','VERY_HIGH',19.20,5000,12,'Nippon India MF',1.68,3,TRUE,TRUE,NOW(),NOW()),
 ('IW-EQ-004','Nifty 50 Index Fund','Passively managed fund replicating the Nifty 50 with minimal tracking error and low cost.','EQUITY','MODERATE',12.40,1000,0,'UTI MF',0.20,5,FALSE,TRUE,NOW(),NOW()),
 ('IW-EQ-005','ELSS Tax Saver Fund','Equity linked savings scheme with the shortest lock-in among 80C instruments.','ELSS','HIGH',14.70,500,36,'Quant MF',1.75,4,FALSE,TRUE,NOW(),NOW()),
 ('IW-DB-001','Corporate Bond Fund','AA+ and above rated corporate debt. Stable accrual income with low duration risk.','DEBT','LOW',7.60,5000,0,'ICICI Prudential AMC',0.58,4,FALSE,TRUE,NOW(),NOW()),
 ('IW-DB-002','Liquid Overnight Fund','Parking vehicle for an emergency corpus with next-day redemption.','DEBT','VERY_LOW',6.40,1000,0,'SBI MF',0.16,5,FALSE,TRUE,NOW(),NOW()),
 ('IW-DB-003','Partner Fixed Deposit','Corporate fixed deposit, AAA rated. Guaranteed returns, no market linkage.','DEBT','VERY_LOW',7.25,10000,12,'Bajaj Finance',NULL,5,FALSE,TRUE,NOW(),NOW()),
 ('IW-DB-004','Public Provident Fund','Government-backed 15 year scheme with EEE tax treatment.','DEBT','VERY_LOW',7.10,500,180,'Government of India',NULL,5,FALSE,TRUE,NOW(),NOW()),
 ('IW-DB-005','National Pension System','Retirement scheme with equity/debt auto-choice and an extra 50,000 deduction.','DEBT','MODERATE',10.20,1000,180,'PFRDA',0.09,4,FALSE,TRUE,NOW(),NOW()),
 ('IW-GD-001','Sovereign Gold Bond','RBI issued bond paying 2.5% annual interest on top of gold price appreciation.','GOLD','LOW',9.80,5000,60,'Reserve Bank of India',NULL,4,FALSE,TRUE,NOW(),NOW()),
 ('IW-GD-002','Gold ETF','Exchange traded fund tracking domestic gold prices. Liquid inflation hedge.','GOLD','LOW',9.10,1000,0,'Nippon India MF',0.32,4,FALSE,TRUE,NOW(),NOW()),
 ('IW-HY-001','Balanced Advantage Fund','Shifts between equity and debt based on valuations. Smoother ride across cycles.','HYBRID','MODERATE',11.60,5000,0,'Kotak AMC',1.12,4,FALSE,TRUE,NOW(),NOW()),
 ('IW-HY-002','Aggressive Hybrid Fund','65-80% equity with a debt cushion. A first step up from fixed deposits.','HYBRID','MODERATE',12.90,5000,0,'Mirae Asset',1.24,4,FALSE,TRUE,NOW(),NOW()),
 ('IW-RE-001','REIT Income Portfolio','Commercial real estate trust generating rental yield plus capital appreciation.','REAL_ESTATE','HIGH',11.20,15000,0,'Embassy REIT',NULL,3,TRUE,TRUE,NOW(),NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT INTO plans (code,name,description,tier,price,duration_months,features,max_goals,active,created_at,updated_at)
VALUES
 ('FREE','Starter','Everything you need to begin investing with intent.','FREE',0,12,
  'Up to 3 financial goals|Risk assessment|Product catalogue|Portfolio tracking|Educational articles',3,TRUE,NOW(),NOW()),
 ('PREMIUM_M','Premium Monthly','Advanced analytics and unlimited planning, billed monthly.','PREMIUM',499,1,
  'Unlimited goals|Personalised recommendations|Premium-only products|Advanced analytics|PDF reports|Priority support',999,TRUE,NOW(),NOW()),
 ('PREMIUM_Y','Premium Annual','The full InvestWise experience at two months free.','PREMIUM',4999,12,
  'Everything in Premium Monthly|Portfolio health review|Tax insights|Rebalancing plan|Priority support',999,TRUE,NOW(),NOW()),
 ('ELITE_Y','Elite Annual','White-glove planning with a dedicated advisory desk.','ELITE',14999,12,
  'Everything in Premium Annual|Dedicated advisor|Quarterly rebalancing calls|Concierge onboarding',999,TRUE,NOW(),NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT INTO articles (title,slug,summary,content,category,author,read_minutes,premium_only,published,view_count,created_at,updated_at)
VALUES
 ('The Power of Compounding, Explained Simply','power-of-compounding',
  'Why starting five years earlier can matter more than investing twice as much.',
  'Compounding is the process by which the returns you earn begin earning returns of their own. A 5,000 rupee monthly SIP at 12% annualised grows to roughly 11.6 lakh in 10 years, 50 lakh in 20 years and 1.76 crore in 30 years. Notice that the third decade alone adds more than the first two combined.\n\nThat asymmetry is why time in the market is a more reliable lever than the size of the contribution. Start now with whatever amount is sustainable rather than waiting for a larger amount later, avoid interrupting the chain by redeeming for discretionary spending, and keep costs low, because an expense ratio of 1.5% versus 0.2% compounds against you with exactly the same mathematics.',
  'BASICS','InvestWise Research Desk',6,FALSE,TRUE,1284,NOW(),NOW()),
 ('Asset Allocation Before Stock Selection','asset-allocation-first',
  'Studies attribute most of a portfolio''s return variability to allocation, not to individual picks.',
  'Asset allocation is the decision of how much of your money sits in equity, debt, gold and cash. It is made before you choose a single fund, and it explains far more of your outcome than which large-cap fund you selected.\n\nA useful starting heuristic is to hold your age in debt as a percentage, adjusted for your actual risk capacity and horizon. A 30 year old with a stable income, an emergency fund and a 20 year horizon can reasonably run 70-80% equity. The same person six months from a home down payment should hold that money in liquid debt regardless of age.\n\nRebalance annually: sell what has run up, buy what has lagged, and let the discipline rather than the forecast drive the trade.',
  'STRATEGY','InvestWise Research Desk',8,FALSE,TRUE,932,NOW(),NOW()),
 ('SIP vs Lumpsum: What the Numbers Say','sip-vs-lumpsum',
  'Lumpsum wins more often on average. SIP wins more often on behaviour.',
  'Because markets rise more often than they fall, deploying a lumpsum immediately beats staggering it roughly two-thirds of the time in backtests. That is the mathematical answer.\n\nThe behavioural answer is different: most investors cannot tolerate deploying their savings one week before a 20% drawdown, and the resulting panic redemption destroys more value than the staggering ever cost. A systematic investment plan converts an emotionally loaded timing decision into an automated instruction, which is precisely its value.\n\nUse lumpsum when the money is windfall capital you will not miss and your horizon exceeds seven years. Use SIP for salary-linked surplus, which is the situation most investors are actually in.',
  'STRATEGY','InvestWise Research Desk',7,FALSE,TRUE,1567,NOW(),NOW()),
 ('Reading a Mutual Fund Factsheet','reading-a-factsheet',
  'The five numbers that actually matter on the monthly one-pager.',
  'Start with the expense ratio, because it is the only number on the sheet that is guaranteed to occur. Then look at portfolio turnover: consistently high turnover in a fund marketed as long-term is a contradiction worth questioning.\n\nThird, check the top-10 concentration; above 55% you are buying a concentrated bet, which is fine if intended. Fourth, examine rolling returns rather than point-to-point returns, since point-to-point figures are hostage to the start date.\n\nFifth, read the fund manager tenure. A five star rating earned by a manager who left eighteen months ago tells you about history, not about the portfolio you are buying today.',
  'PRODUCTS','InvestWise Research Desk',9,FALSE,TRUE,704,NOW(),NOW()),
 ('Building Your Emergency Fund First','emergency-fund-first',
  'The unglamorous prerequisite that decides whether your plan survives contact with reality.',
  'An emergency fund is not an investment, it is insurance for your investments. Without it, the first medical bill or job gap forces a redemption at exactly the wrong moment, converting a paper loss into a realised one and breaking the compounding chain.\n\nTarget six months of essential expenses, extended to nine or twelve if you are self-employed or a single earner supporting dependents. Hold it in a liquid or overnight fund, or a sweep-in fixed deposit, where the objective is same-day access rather than return.\n\nDo not hold it in equity, do not hold it in a five year lock-in, and do not count your credit card limit as an emergency fund.',
  'BASICS','InvestWise Research Desk',5,FALSE,TRUE,2011,NOW(),NOW()),
 ('Tax-Efficient Withdrawal Sequencing','tax-efficient-withdrawal',
  'A premium deep-dive on drawing down a portfolio without donating returns to avoidable tax.',
  'Withdrawal sequencing determines how much of your accumulated corpus you actually keep. Equity held beyond twelve months attracts long-term capital gains tax with an annual exemption threshold, so harvesting gains up to that threshold every financial year resets your cost basis at zero tax cost.\n\nDebt fund gains are taxed at slab rates for units purchased after the 2023 amendment, which changes the ordering: draw from debt in low-income years and from equity in high-income years, not the reverse.\n\nSystematic withdrawal plans are more efficient than lumpsum redemption because each instalment redeems only the units required, leaving the remainder invested and deferring the gain.\n\nFinally, sequence-of-returns risk is real: a bad first three years of retirement withdrawal does permanent damage, which argues for holding two to three years of expenses in liquid debt as a buffer bucket.',
  'TAX','InvestWise Research Desk',12,TRUE,TRUE,318,NOW(),NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();
