import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { contributionSchema, goalSchema } from '../lib/schemas';
import { del, get, post, put, toError } from '../lib/api';
import { useFetch } from '../lib/useFetch';
import { useAuth } from '../lib/auth';
import { Badge, Empty, ErrorBox, Loading, Modal, Notice, PageTitle, Progress } from '../components/Ui';
import { FormError, Input, Select, Submit, TextArea } from '../components/Form';
import { compact, date, money, percent } from '../lib/format';

const PRIORITIES = [
  { value: 'LOW', label: 'Low' }, { value: 'MEDIUM', label: 'Medium' },
  { value: 'HIGH', label: 'High' }, { value: 'CRITICAL', label: 'Critical' },
];

const EMPTY_GOAL = {
  title: '', description: '', goalType: '', targetAmount: '',
  currentAmount: 0, monthlyContribution: 0, targetDate: '', priority: 'MEDIUM',
};

export default function Goals() {
  const { isPremium } = useAuth();
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [contributing, setContributing] = useState(null);
  const [deleting, setDeleting] = useState(null);
  const [error, setError] = useState(null);
  const [notice, setNotice] = useState(null);

  const { data: goals, loading, error: loadError, reload } = useFetch(() => get('/api/v1/goals/all'), []);
  const { data: meta } = useFetch(() => get('/api/v1/metadata'), []);

  const goalForm = useForm({ resolver: zodResolver(goalSchema), mode: 'onChange', defaultValues: EMPTY_GOAL });
  const contributionForm = useForm({ resolver: zodResolver(contributionSchema), mode: 'onChange',
                                     defaultValues: { amount: '', note: '' } });

  const openCreate = () => {
    setEditing(null); setError(null); goalForm.reset(EMPTY_GOAL); setFormOpen(true);
  };

  const openEdit = (goal) => {
    setEditing(goal); setError(null);
    goalForm.reset({
      title: goal.title, description: goal.description ?? '', goalType: goal.goalType,
      targetAmount: goal.targetAmount, currentAmount: goal.currentAmount,
      monthlyContribution: goal.monthlyContribution, targetDate: goal.targetDate, priority: goal.priority,
    });
    setFormOpen(true);
  };

  const submitGoal = async (values) => {
    setError(null);
    try {
      if (editing) {
        await put(`/api/v1/goals/${editing.id}`, values);
        setNotice({ type: 'success', text: 'Goal updated.' });
      } else {
        await post('/api/v1/goals', values);
        setNotice({ type: 'success', text: 'Goal created.' });
      }
      setFormOpen(false);
      reload();
    } catch (err) {
      setError(err.status ? err : toError(err));
    }
  };

  const submitContribution = async (values) => {
    setError(null);
    try {
      const updated = await post(`/api/v1/goals/${contributing.id}/contribute`, values);
      setNotice({
        type: 'success',
        text: updated.status === 'ACHIEVED'
          ? `You reached your target for "${updated.title}".` : 'Contribution recorded.',
      });
      setContributing(null);
      contributionForm.reset();
      reload();
    } catch (err) {
      setError(err.status ? err : toError(err));
    }
  };

  const confirmDelete = async () => {
    try {
      await del(`/api/v1/goals/${deleting.id}`);
      setNotice({ type: 'success', text: 'Goal deleted.' });
      setDeleting(null);
      reload();
    } catch (err) {
      setNotice({ type: 'error', text: toError(err).message });
      setDeleting(null);
    }
  };

  // Pre-fill the target date from the goal type's typical horizon
  const onTypeChange = (event) => {
    const selected = meta?.goalTypes?.find((t) => t.value === event.target.value);
    if (selected && !goalForm.getValues('targetDate')) {
      const target = new Date();
      target.setFullYear(target.getFullYear() + selected.horizonYears);
      goalForm.setValue('targetDate', target.toISOString().slice(0, 10), { shouldValidate: true });
    }
  };

  if (loading) return <Loading label="Loading your goals…" />;
  if (loadError) return <ErrorBox error={loadError} onRetry={reload} />;

  const atFreeLimit = !isPremium && (goals?.length ?? 0) >= 3;

  return (
    <div className="space-y-6">
      <PageTitle title="Financial goals"
                 subtitle="Each goal is inflated to future rupees and tracked against a linear glide path."
                 action={<button type="button" onClick={openCreate} className="btn-primary" disabled={atFreeLimit}>
                           New goal</button>} />

      {notice && (
        <Notice type={notice.type} onDismiss={() => setNotice(null)}>{notice.text}</Notice>
      )}

      {atFreeLimit && (
        <div className="card flex flex-wrap items-center justify-between gap-4 border-l-4 border-l-amber-400">
          <div>
            <p className="font-semibold text-slate-900">You have used all three free goals</p>
            <p className="mt-0.5 text-sm text-slate-600">
              Premium removes the cap. Existing goals stay fully editable either way.
            </p>
          </div>
          <Link to="/pricing" className="btn-primary">Upgrade to Premium</Link>
        </div>
      )}

      {!goals?.length ? (
        <Empty title="No goals yet"
               description="A goal is the anchor for everything else: the engine optimises against it and
                            the reports measure progress towards it."
               action={<button type="button" onClick={openCreate} className="btn-primary">Create your first goal</button>} />
      ) : (
        <div className="grid gap-5 lg:grid-cols-2">
          {goals.map((goal) => (
            <article key={goal.id} className="card">
              <div className="flex flex-wrap items-start justify-between gap-2">
                <div>
                  <h2 className="font-semibold text-slate-900">{goal.title}</h2>
                  <p className="mt-0.5 text-xs text-slate-500">
                    {goal.goalTypeLabel} · {goal.priority.toLowerCase()} priority
                  </p>
                </div>
                <Badge status={goal.status} />
              </div>

              {goal.description && <p className="mt-2 line-clamp-2 text-sm text-slate-600">{goal.description}</p>}

              <div className="mt-4">
                <div className="mb-1 flex justify-between text-xs">
                  <span className="text-slate-600">
                    {compact(goal.currentAmount)} of {compact(goal.targetAmount)}
                  </span>
                  <span className="font-semibold">{percent(goal.progressPct, 1)}</span>
                </div>
                <Progress value={goal.progressPct} />
              </div>

              <dl className="mt-4 grid grid-cols-2 gap-3 border-t border-slate-100 pt-3 text-sm">
                <div>
                  <dt className="text-xs text-slate-400">Target date</dt>
                  <dd className="font-semibold">{date(goal.targetDate)}</dd>
                  <dd className="text-xs text-slate-500">{goal.monthsRemaining} months away</dd>
                </div>
                <div>
                  <dt className="text-xs text-slate-400">Shortfall</dt>
                  <dd className="font-semibold">{compact(goal.shortfall)}</dd>
                </div>
                <div>
                  <dt className="text-xs text-slate-400">Contributing</dt>
                  <dd className="font-semibold">{money(goal.monthlyContribution)}/mo</dd>
                </div>
                <div>
                  <dt className="text-xs text-slate-400">Required</dt>
                  <dd className={`font-semibold ${
                    Number(goal.requiredMonthly) > Number(goal.monthlyContribution)
                      ? 'text-amber-600' : 'text-emerald-600'}`}>
                    {money(goal.requiredMonthly)}/mo
                  </dd>
                </div>
              </dl>

              <p className="mt-3 rounded bg-slate-50 p-3 text-xs text-slate-600">
                Projected to reach <strong>{compact(goal.projectedValue)}</strong> at your current
                contribution. Inflation-adjusted target is <strong>{compact(goal.inflatedTarget)}</strong>.
              </p>

              <div className="mt-4 flex flex-wrap gap-2 border-t border-slate-100 pt-3">
                <button type="button" className="btn-primary flex-1"
                        disabled={goal.status === 'ACHIEVED'}
                        onClick={() => { setContributing(goal); setError(null); contributionForm.reset(); }}>
                  Contribute
                </button>
                <button type="button" onClick={() => openEdit(goal)} className="btn-secondary">Edit</button>
                <button type="button" onClick={() => setDeleting(goal)}
                        className="btn-secondary text-red-600">Delete</button>
              </div>
            </article>
          ))}
        </div>
      )}

      {/* ---------- create / edit ---------- */}
      <Modal open={formOpen} onClose={() => setFormOpen(false)}
             title={editing ? 'Edit goal' : 'Create a financial goal'}
             footer={<>
               <button type="button" onClick={() => setFormOpen(false)} className="btn-secondary">Cancel</button>
               <Submit form="goal-form" loading={goalForm.formState.isSubmitting}
                       disabled={!goalForm.formState.isValid}>
                 {editing ? 'Save changes' : 'Create goal'}
               </Submit>
             </>}>
        <form id="goal-form" onSubmit={goalForm.handleSubmit(submitGoal)} noValidate className="space-y-4">
          <FormError error={error} />

          <Input label="Goal title" required placeholder="Daughter's college fund"
                 error={goalForm.formState.errors.title} {...goalForm.register('title')} />

          <TextArea label="Description" rows={2} placeholder="Four year undergraduate programme"
                    error={goalForm.formState.errors.description} {...goalForm.register('description')} />

          <div className="grid gap-4 sm:grid-cols-2">
            <Select label="Goal type" required placeholder="Choose a type"
                    options={meta?.goalTypes?.map((t) => ({ value: t.value, label: t.label })) ?? []}
                    error={goalForm.formState.errors.goalType}
                    {...goalForm.register('goalType', { onChange: onTypeChange })} />
            <Select label="Priority" options={PRIORITIES}
                    error={goalForm.formState.errors.priority} {...goalForm.register('priority')} />
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <Input label="Target amount" required type="number" min="1000" step="1000" placeholder="2500000"
                   hint="In today's rupees. We inflate it for you."
                   error={goalForm.formState.errors.targetAmount} {...goalForm.register('targetAmount')} />
            <Input label="Target date" required type="date" hint="At least one month away"
                   error={goalForm.formState.errors.targetDate} {...goalForm.register('targetDate')} />
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <Input label="Already saved" type="number" min="0" step="1000" placeholder="150000"
                   error={goalForm.formState.errors.currentAmount} {...goalForm.register('currentAmount')} />
            <Input label="Monthly contribution" type="number" min="0" step="500" placeholder="15000"
                   error={goalForm.formState.errors.monthlyContribution}
                   {...goalForm.register('monthlyContribution')} />
          </div>
        </form>
      </Modal>

      {/* ---------- contribute ---------- */}
      <Modal open={Boolean(contributing)} onClose={() => setContributing(null)}
             title={`Contribute to "${contributing?.title ?? ''}"`}
             footer={<>
               <button type="button" onClick={() => setContributing(null)} className="btn-secondary">Cancel</button>
               <Submit form="contribution-form" loading={contributionForm.formState.isSubmitting}
                       disabled={!contributionForm.formState.isValid}>Record contribution</Submit>
             </>}>
        <form id="contribution-form" onSubmit={contributionForm.handleSubmit(submitContribution)}
              noValidate className="space-y-4">
          <FormError error={error} />

          {contributing && (
            <div className="rounded bg-slate-50 p-3 text-sm">
              <div className="flex justify-between">
                <span className="text-slate-600">Currently saved</span>
                <span className="font-semibold">{money(contributing.currentAmount)}</span>
              </div>
              <div className="mt-1 flex justify-between">
                <span className="text-slate-600">Remaining to target</span>
                <span className="font-semibold">{money(contributing.shortfall)}</span>
              </div>
            </div>
          )}

          <Input label="Amount" required type="number" min="1" step="500" placeholder="10000" autoFocus
                 error={contributionForm.formState.errors.amount} {...contributionForm.register('amount')} />
          <Input label="Note" placeholder="July SIP instalment" hint="Optional"
                 error={contributionForm.formState.errors.note} {...contributionForm.register('note')} />
        </form>
      </Modal>

      {/* ---------- delete ---------- */}
      <Modal open={Boolean(deleting)} onClose={() => setDeleting(null)} title="Delete this goal?"
             footer={<>
               <button type="button" onClick={() => setDeleting(null)} className="btn-secondary">Cancel</button>
               <button type="button" onClick={confirmDelete} className="btn-danger">Delete permanently</button>
             </>}>
        <p className="text-sm leading-relaxed text-slate-600">
          &ldquo;{deleting?.title}&rdquo; will be removed along with its recommendations. Holdings
          linked to this goal are not deleted; they simply become unlinked. This cannot be undone.
        </p>
      </Modal>
    </div>
  );
}
