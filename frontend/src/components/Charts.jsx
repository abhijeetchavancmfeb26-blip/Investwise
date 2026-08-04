import {
  ArcElement, BarElement, CategoryScale, Chart, Filler, Legend,
  LinearScale, LineElement, PointElement, Tooltip,
} from 'chart.js';
import { Bar, Doughnut, Line } from 'react-chartjs-2';
import { compact } from '../lib/format';

// Register only what is used, which keeps the bundle smaller than auto-registration.
Chart.register(ArcElement, BarElement, CategoryScale, LinearScale,
  LineElement, PointElement, Filler, Tooltip, Legend);

const COLOURS = ['#0f766e', '#14b8a6', '#5eead4', '#0ea5e9', '#6366f1',
                 '#a855f7', '#f59e0b', '#ef4444', '#84cc16', '#64748b'];

const BASE = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { position: 'bottom', labels: { boxWidth: 10, padding: 12, font: { size: 11 } } },
  },
};

/** Chart.js needs a sized container, so every chart is wrapped in one. */
export function ChartBox({ title, subtitle, height = 'h-64', children }) {
  return (
    <div className="card">
      <h3 className="text-sm font-semibold text-slate-800">{title}</h3>
      {subtitle && <p className="mt-0.5 text-xs text-slate-500">{subtitle}</p>}
      <div className={`mt-4 ${height}`}>{children}</div>
    </div>
  );
}

function NoData() {
  return <p className="flex h-full items-center justify-center text-sm text-slate-400">No data yet</p>;
}

export function DoughnutChart({ data = {}, suffix = '%' }) {
  const labels = Object.keys(data ?? {});
  if (labels.length === 0) return <NoData />;

  return (
    <Doughnut
      data={{
        labels: labels.map((l) => l.replace(/_/g, ' ')),
        datasets: [{ data: Object.values(data).map(Number), backgroundColor: COLOURS, borderWidth: 2 }],
      }}
      options={{
        ...BASE,
        cutout: '60%',
        plugins: {
          ...BASE.plugins,
          tooltip: { callbacks: { label: (ctx) => ` ${ctx.label}: ${ctx.parsed}${suffix}` } },
        },
      }}
    />
  );
}

export function LineChart({ data = {}, label = 'Value', currency = true }) {
  const labels = Object.keys(data ?? {});
  if (labels.length === 0) return <NoData />;

  return (
    <Line
      data={{
        labels,
        datasets: [{
          label,
          data: Object.values(data).map(Number),
          borderColor: '#0f766e',
          backgroundColor: 'rgba(20,184,166,0.15)',
          fill: true,
          tension: 0.3,
          pointRadius: 2,
        }],
      }}
      options={{
        ...BASE,
        scales: {
          y: { ticks: { callback: (v) => (currency ? compact(v) : v), font: { size: 11 } } },
          x: { grid: { display: false }, ticks: { font: { size: 10 } } },
        },
      }}
    />
  );
}

export function BarChart({ data = {}, label = 'Value', currency = false, horizontal = false }) {
  const labels = Object.keys(data ?? {});
  if (labels.length === 0) return <NoData />;

  return (
    <Bar
      data={{
        labels: labels.map((l) => l.replace(/_/g, ' ')),
        datasets: [{ label, data: Object.values(data).map(Number), backgroundColor: COLOURS, borderRadius: 4 }],
      }}
      options={{
        ...BASE,
        indexAxis: horizontal ? 'y' : 'x',
        plugins: { ...BASE.plugins, legend: { display: false } },
        scales: {
          y: { beginAtZero: true, ticks: { callback: (v) => (currency ? compact(v) : v), font: { size: 11 } } },
          x: { grid: { display: false }, ticks: { font: { size: 10 } } },
        },
      }}
    />
  );
}
