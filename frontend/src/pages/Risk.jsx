import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { riskSchema } from '../lib/schemas';
import { get, post, toError } from '../lib/api';
import { useFetch } from '../lib/useFetch';
import { ErrorBox, Loading, PageTitle, Progress } from '../components/Ui';
import { BarChart, ChartBox, DoughnutChart } from '../components/Charts';
import { Checkbox, FormError, Input, Select, Submit } from '../components/Form';
import { title } from '../lib/format';

export default function Risk() {
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);
  const [retaking, setRetaking] = useState(false);

  const { data: meta } = useFetch(() => get('/api/v1/metadata'), []);
  const { data: current, loading, error: loadError, reload } = useFetch(() => get('/api/v1/risk/me'), []);

  const { register, handleSubmit, formState: { errors, isSubmitting, isValid } } =
    useForm({
      resolver: zodResolver(riskSchema), mode: 'onChange',
      defaultValues: {
        age: '', annualIncome: '', monthlySurplus: '', dependents: 0, horizonYears: '',
        knowledgeLevel: '', lossTolerance: '', hasEmergencyFund: false, hasHealthInsurance: false,
      },
    });

  const onSubmit = async (values) => {
    setError(null);
    try {
      setResult(await post('/api/v1/risk/assess', values));
      setRetaking(false);
      reload();
    } catch (err) {
      setError(err.status ? err : toError(err));
    }
  };

  if (loading) return <Loading label="Loading your risk profile…" />;
  if (loadError && loadError.status !== 404) return <ErrorBox error={loadError} onRetry={reload} />;

  const active = result ?? current;
  const showForm = !active || retaking;

  return (
    <div className="space-y-6">
      <PageTitle title="Risk assessment"
                 subtitle="Seven factors totalling 100. Four measure what you can afford to lose, two what
                           you are willing to lose, and one your safety net."
                 action={active && !retaking && (
                   <button type="button" onClick={() => setRetaking(true)} className="btn-secondary">
                     Retake the questionnaire
                   </button>
                 )} />

      {/* ---------- result ---------- */}
      {active && !retaking && (
        <>
          <div className="card">
            <div className="flex flex-wrap items-start justify-between gap-6">
              <div className="min-w-0 flex-1">
                <p className="text-xs font-bold uppercase text-brand">Your profile</p>
                <h2 className="mt-1 text-3xl font-bold text-slate-900">{title(active.profile)}</h2>
                <p className="mt-3 max-w-2xl leading-relaxed text-slate-600">{active.summary}</p>
              </div>
              <div className="text-center">
                <p className="text-3xl font-bold text-slate-900">{active.score}</p>
                <p className="text-xs uppercase text-slate-400">of 100</p>
              </div>
            </div>
          </div>

          <div className="grid gap-6 lg:grid-cols-2">
            <ChartBox title="Recommended strategic allocation"
                      subtitle="The split the engine will target for you">
              <DoughnutChart data={{
                Equity: active.equityPct, Debt: active.debtPct, Gold: active.goldPct,
              }} />
            </ChartBox>

            {active.breakdown && Object.keys(active.breakdown).length > 0 ? (
              <ChartBox title="What drove your score"
                        subtitle="Each factor's contribution out of its own maximum">
                <BarChart data={active.breakdown} label="Points" horizontal />
              </ChartBox>
            ) : (
              <div className="card">
                <h3 className="text-sm font-semibold text-slate-800">Allocation detail</h3>
                <div className="mt-4 space-y-4">
                  {[['Equity', active.equityPct], ['Debt', active.debtPct], ['Gold', active.goldPct]]
                    .map(([label, value]) => (
                      <div key={label}>
                        <div className="mb-1 flex justify-between text-sm">
                          <span className="text-slate-600">{label}</span>
                          <span className="font-semibold">{value}%</span>
                        </div>
                        <Progress value={value} />
                      </div>
                    ))}
                </div>
              </div>
            )}
          </div>

          {active.guidance?.length > 0 && (
            <div className="card">
              <h3 className="font-semibold text-slate-900">What to do about it</h3>
              <ul className="mt-4 space-y-3">
                {active.guidance.map((item, index) => (
                  <li key={index} className="rounded bg-slate-50 p-3 text-sm leading-relaxed text-slate-700">
                    {item}
                  </li>
                ))}
              </ul>
            </div>
          )}

          <div className="card flex flex-wrap items-center justify-between gap-4">
            <div>
              <h3 className="font-semibold text-slate-900">Ready for a recommendation?</h3>
              <p className="mt-1 text-sm text-slate-600">
                The engine will score the catalogue against this profile and allocate your investable amount.
              </p>
            </div>
            <Link to="/recommendations" className="btn-primary">Generate recommendations</Link>
          </div>
        </>
      )}

      {/* ---------- questionnaire ---------- */}
      {showForm && (
        <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-5">
          <FormError error={error} />

          <fieldset className="card">
            <legend className="text-sm font-bold uppercase text-brand">Your circumstances</legend>
            <p className="mt-1 text-sm text-slate-500">
              These four measure risk <em>capacity</em>: how much of a fall your finances can absorb
              without forcing you to sell.
            </p>

            <div className="mt-4 grid gap-4 sm:grid-cols-2">
              <Input label="Your age" required type="number" min="18" max="100" placeholder="31" autoFocus
                     error={errors.age} {...register('age')} />
              <Input label="Investment horizon (years)" required type="number" min="1" max="40"
                     placeholder="15" hint="How long before you need this money"
                     error={errors.horizonYears} {...register('horizonYears')} />
              <Input label="Annual income" required type="number" min="0" step="10000" placeholder="1200000"
                     error={errors.annualIncome} {...register('annualIncome')} />
              <Input label="Monthly investable surplus" required type="number" min="0" step="1000"
                     placeholder="25000" hint="What is genuinely left after expenses and EMIs"
                     error={errors.monthlySurplus} {...register('monthlySurplus')} />
              <Input label="Financial dependents" type="number" min="0" max="15" placeholder="2"
                     hint="People who rely on your income"
                     error={errors.dependents} {...register('dependents')} />
            </div>
          </fieldset>

          <fieldset className="card">
            <legend className="text-sm font-bold uppercase text-brand">Your temperament</legend>
            <p className="mt-1 text-sm text-slate-500">
              These measure risk <em>tolerance</em>. Answer honestly rather than aspirationally; the
              point is a portfolio you will actually hold through a bad year.
            </p>

            <div className="mt-4 grid gap-4 sm:grid-cols-2">
              <Select label="Market experience" required placeholder="Select your level"
                      options={meta?.knowledgeLevels?.map((l) => ({ value: l, label: title(l) })) ?? []}
                      error={errors.knowledgeLevel} {...register('knowledgeLevel')} />
              <Select label="If your portfolio fell 20% in a month, you would…" required
                      placeholder="Select the closest answer"
                      options={meta?.lossTolerances ?? []}
                      error={errors.lossTolerance} {...register('lossTolerance')} />
            </div>
          </fieldset>

          <fieldset className="card">
            <legend className="text-sm font-bold uppercase text-brand">Your safety net</legend>
            <p className="mt-1 text-sm text-slate-500">
              Without these, any market fall can force a redemption at the worst moment, which is what
              turns a paper loss into a real one.
            </p>

            <div className="mt-4 space-y-3">
              <Checkbox label="I hold at least six months of essential expenses in liquid savings"
                        {...register('hasEmergencyFund')} />
              <Checkbox label="I and my dependents have health insurance cover"
                        {...register('hasHealthInsurance')} />
            </div>
          </fieldset>

          <div className="flex flex-wrap gap-3">
            <Submit loading={isSubmitting} disabled={!isValid}>Calculate my risk profile</Submit>
            {retaking && (
              <button type="button" onClick={() => setRetaking(false)} className="btn-secondary">Cancel</button>
            )}
          </div>

          <p className="text-xs leading-relaxed text-slate-500">
            Every assessment is stored rather than overwritten, so you can see how your profile has
            changed. Retake it annually, or sooner if your circumstances change materially.
          </p>
        </form>
      )}
    </div>
  );
}
