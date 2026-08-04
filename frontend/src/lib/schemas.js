import { z } from 'zod';

/**
 * Zod schemas mirroring the server's Bean Validation constraints exactly.
 *
 * Client validation exists for the user's benefit, not the server's: the backend
 * re-validates everything, and these rules simply avoid a round trip to discover
 * something the browser already knew.
 */

const NAME = /^[A-Za-z][A-Za-z .'-]{1,49}$/;
const PHONE = /^[6-9]\d{9}$/;
const PAN = /^[A-Z]{5}[0-9]{4}[A-Z]$/;
const PINCODE = /^[1-9][0-9]{5}$/;

const name = (label) => z.string().trim()
  .min(2, `${label} must be at least 2 characters`)
  .max(50, `${label} must not exceed 50 characters`)
  .regex(NAME, `${label} may contain only letters, spaces, apostrophes and hyphens`);

const email = z.string().trim().min(1, 'Email is required')
  .email('Please enter a valid email address')
  .transform((value) => value.toLowerCase());

const phone = z.string().trim().regex(PHONE, 'Enter a valid 10 digit number starting 6-9');

const optionalPan = z.string().trim().toUpperCase()
  .refine((v) => v === '' || PAN.test(v), 'PAN must look like ABCPE1234F')
  .optional().or(z.literal(''));

/** Same seven rules the server's StrongPassword validator applies. */
export const password = z.string()
  .min(8, 'At least 8 characters')
  .max(64, 'At most 64 characters')
  .regex(/[a-z]/, 'Needs a lowercase letter')
  .regex(/[A-Z]/, 'Needs an uppercase letter')
  .regex(/\d/, 'Needs a digit')
  .regex(/[@$!%*?&#^()\-_=+]/, 'Needs a special character')
  .refine((v) => !/\s/.test(v), 'No spaces')
  .refine((v) => !['password', 'password1', 'password@123', 'admin@123', 'welcome@123',
    'qwerty123', '12345678', 'investwise', 'invest@123', 'india@123'].includes(v.toLowerCase()),
    'That password is too common');

const adult = (value) => {
  if (!value) return true;
  const dob = new Date(value);
  const now = new Date();
  let age = now.getFullYear() - dob.getFullYear();
  const months = now.getMonth() - dob.getMonth();
  if (months < 0 || (months === 0 && now.getDate() < dob.getDate())) age -= 1;
  return age >= 18;
};

export const registerSchema = z.object({
  firstName: name('First name'),
  lastName: name('Last name'),
  email,
  password,
  confirmPassword: z.string().min(1, 'Please confirm your password'),
  phone,
  dateOfBirth: z.string().min(1, 'Date of birth is required').refine(adult, 'You must be at least 18'),
  gender: z.string().optional().or(z.literal('')),
  panNumber: optionalPan,
  annualIncome: z.coerce.number().min(0).optional().or(z.literal('')),
  occupation: z.string().max(100).optional().or(z.literal('')),
  acceptTerms: z.literal(true, { errorMap: () => ({ message: 'You must accept the terms' }) }),
}).refine((d) => d.password === d.confirmPassword, {
  message: 'Passwords do not match', path: ['confirmPassword'],
});

export const loginSchema = z.object({
  email,
  password: z.string().min(1, 'Password is required'),
  rememberMe: z.boolean().optional(),
});

export const forgotSchema = z.object({ email });

export const resetSchema = z.object({
  token: z.string().min(1),
  newPassword: password,
  confirmPassword: z.string().min(1, 'Please confirm your new password'),
}).refine((d) => d.newPassword === d.confirmPassword, {
  message: 'Passwords do not match', path: ['confirmPassword'],
});

export const changePasswordSchema = z.object({
  currentPassword: z.string().min(1, 'Your current password is required'),
  newPassword: password,
  confirmPassword: z.string().min(1, 'Please confirm your new password'),
}).refine((d) => d.newPassword === d.confirmPassword, {
  message: 'Passwords do not match', path: ['confirmPassword'],
}).refine((d) => d.currentPassword !== d.newPassword, {
  message: 'The new password must differ from your current one', path: ['newPassword'],
});

export const profileSchema = z.object({
  firstName: name('First name'),
  lastName: name('Last name'),
  phone,
  dateOfBirth: z.string().refine(adult, 'You must be at least 18').optional().or(z.literal('')),
  gender: z.string().optional().or(z.literal('')),
  panNumber: optionalPan,
  annualIncome: z.coerce.number().min(0).optional().or(z.literal('')),
  occupation: z.string().max(100).optional().or(z.literal('')),
  address: z.string().max(300).optional().or(z.literal('')),
  city: z.string().max(60).optional().or(z.literal('')),
  state: z.string().max(60).optional().or(z.literal('')),
  pincode: z.string().regex(PINCODE, 'Enter a valid 6 digit PIN code').optional().or(z.literal('')),
});

export const contactSchema = z.object({
  name: z.string().trim().min(2, 'Name must be at least 2 characters').max(80),
  email,
  phone: z.string().trim().regex(PHONE, 'Enter a valid 10 digit number').optional().or(z.literal('')),
  subject: z.string().trim().min(3, 'Subject must be at least 3 characters').max(150),
  message: z.string().trim().min(10, 'Please write at least 10 characters').max(2000),
});

export const goalSchema = z.object({
  title: z.string().trim().min(3, 'Title must be at least 3 characters').max(120),
  description: z.string().max(500).optional().or(z.literal('')),
  goalType: z.string().min(1, 'Choose a goal type'),
  targetAmount: z.coerce.number().min(1000, 'Target must be at least ₹1,000'),
  currentAmount: z.coerce.number().min(0).optional(),
  monthlyContribution: z.coerce.number().min(0).optional(),
  targetDate: z.string().min(1, 'Choose a target date').refine((value) => {
    const target = new Date(value);
    const minimum = new Date();
    minimum.setMonth(minimum.getMonth() + 1);
    return target >= minimum;
  }, 'The target date must be at least one month away'),
  priority: z.string().optional(),
}).refine((d) => !d.currentAmount || d.currentAmount <= d.targetAmount, {
  message: 'Amount saved cannot exceed the target', path: ['currentAmount'],
});

export const contributionSchema = z.object({
  amount: z.coerce.number().min(1, 'Contribution must be at least ₹1'),
  note: z.string().max(200).optional().or(z.literal('')),
});

export const riskSchema = z.object({
  age: z.coerce.number().int().min(18, 'You must be at least 18').max(100),
  annualIncome: z.coerce.number().min(0),
  monthlySurplus: z.coerce.number().min(0),
  dependents: z.coerce.number().int().min(0).max(15).optional(),
  horizonYears: z.coerce.number().int().min(1, 'At least 1 year').max(40),
  knowledgeLevel: z.string().min(1, 'Select your experience level'),
  lossTolerance: z.string().min(1, 'Tell us how you would react'),
  hasEmergencyFund: z.boolean().optional(),
  hasHealthInsurance: z.boolean().optional(),
});

export const recommendSchema = z.object({
  goalId: z.string().optional().or(z.literal('')),
  investableAmount: z.coerce.number().min(500, 'At least ₹500'),
  horizonYears: z.coerce.number().int().min(1).max(40).optional().or(z.literal('')),
});

export const holdingSchema = z.object({
  productId: z.string().min(1, 'Choose a product'),
  goalId: z.string().optional().or(z.literal('')),
  amount: z.coerce.number().min(100, 'At least ₹100'),
  buyPrice: z.coerce.number().min(0.0001, 'Price must be greater than zero'),
  purchaseDate: z.string().min(1, 'Choose the purchase date')
    .refine((v) => new Date(v) <= new Date(), 'The purchase date cannot be in the future'),
});

export const redeemSchema = z.object({
  units: z.coerce.number().min(0.0001, 'Units must be greater than zero'),
});

export const productSchema = z.object({
  code: z.string().regex(/^[A-Z]{2}-[A-Z]{2}-\d{3}$/, 'Code must look like IW-EQ-001'),
  name: z.string().trim().min(3).max(150),
  description: z.string().max(4000).optional().or(z.literal('')),
  category: z.string().min(1, 'Choose a category'),
  riskLevel: z.string().min(1, 'Choose a risk level'),
  expectedReturn: z.coerce.number().min(0).max(60, 'Above 60% is not credible'),
  minInvestment: z.coerce.number().min(100),
  lockInMonths: z.coerce.number().int().min(0).max(360).optional(),
  fundHouse: z.string().max(120).optional().or(z.literal('')),
  expenseRatio: z.coerce.number().min(0).max(5).optional().or(z.literal('')),
  rating: z.coerce.number().int().min(1).max(5),
  premiumOnly: z.boolean().optional(),
  active: z.boolean().optional(),
});

export const articleSchema = z.object({
  title: z.string().trim().min(5).max(200),
  summary: z.string().max(500).optional().or(z.literal('')),
  content: z.string().min(50, 'Content must be at least 50 characters'),
  category: z.string().min(1, 'Choose a category'),
  author: z.string().max(120).optional().or(z.literal('')),
  readMinutes: z.coerce.number().int().min(1).max(120).optional(),
  premiumOnly: z.boolean().optional(),
  published: z.boolean().optional(),
});

/** Scores a password 0-5 for the strength meter, using the same rules as above. */
export const scorePassword = (value = '') => {
  const checks = [
    { label: 'At least 8 characters', ok: value.length >= 8 },
    { label: 'An uppercase letter', ok: /[A-Z]/.test(value) },
    { label: 'A lowercase letter', ok: /[a-z]/.test(value) },
    { label: 'A number', ok: /\d/.test(value) },
    { label: 'A special character', ok: /[@$!%*?&#^()\-_=+]/.test(value) },
  ];
  const score = checks.filter((c) => c.ok).length;
  const meta = [
    { label: 'Very weak', bar: 'bg-red-500', text: 'text-red-600' },
    { label: 'Weak', bar: 'bg-red-400', text: 'text-red-600' },
    { label: 'Fair', bar: 'bg-amber-400', text: 'text-amber-600' },
    { label: 'Good', bar: 'bg-yellow-400', text: 'text-yellow-700' },
    { label: 'Strong', bar: 'bg-emerald-500', text: 'text-emerald-600' },
    { label: 'Very strong', bar: 'bg-emerald-600', text: 'text-emerald-700' },
  ][score];
  return { score, checks, ...meta };
};
