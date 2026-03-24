import { Component, inject, OnInit, signal, OnDestroy } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import {
  DashboardService,
  DashboardEstadisticas,
  PrestamosPorMes,
  EquiposPorCategoria,
} from '../../core/services/dashboard.service';
import { PrestamoService } from '../../core/services/prestamo.service';
import type { Prestamo } from '../../core/models/prestamo.model';
import { Chart, registerables } from 'chart.js';

Chart.register(...registerables);

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, DatePipe],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent implements OnInit, OnDestroy {
  private dashboardService = inject(DashboardService);
  private prestamoService = inject(PrestamoService);
  auth = inject(AuthService);

  stats = signal<DashboardEstadisticas | null>(null);
  prestamosPorMes = signal<PrestamosPorMes[]>([]);
  equiposPorCategoria = signal<EquiposPorCategoria[]>([]);
  ultimosPrestamos = signal<Prestamo[]>([]);
  loading = signal(true);
  error = signal('');
  isAdmin = this.auth.isAdmin;
  lastUpdated = signal(new Date());

  private chartPrestamos: Chart | null = null;
  private chartCategoria: Chart | null = null;

  ngOnInit() {
    this.load();
  }

  ngOnDestroy() {
    this.chartPrestamos?.destroy();
    this.chartCategoria?.destroy();
  }

  load() {
    this.loading.set(true);
    this.error.set('');
    this.lastUpdated.set(new Date());

    this.dashboardService.getEstadisticas().subscribe({
      next: (data) => {
        this.stats.set(data);
        this.loading.set(false);

        if (this.auth.isAdmin()) {
          this.loadGraficos();
        }
        this.loadUltimosPrestamos();
      },
      error: (err) => {
        this.error.set(err.error?.message ?? 'Error al cargar estadísticas.');
        this.loading.set(false);
      },
    });
  }

  private loadGraficos() {
    this.dashboardService.getPrestamosPorMes().subscribe({
      next: (list) => {
        this.prestamosPorMes.set(list);
        setTimeout(() => this.renderBarChart(), 80);
      },
    });
    this.dashboardService.getEquiposPorCategoria().subscribe({
      next: (list) => {
        this.equiposPorCategoria.set(list);
        setTimeout(() => this.renderDoughnutChart(), 80);
      },
    });
  }

  private loadUltimosPrestamos() {
    this.prestamoService.listarTodos().subscribe({
      next: (list) => {
        const sorted = [...list].sort(
          (a, b) => new Date(b.fechaHoraSolicitud).getTime() - new Date(a.fechaHoraSolicitud).getTime()
        );
        this.ultimosPrestamos.set(sorted.slice(0, 5));
      },
      error: () => {},
    });
  }

  private renderBarChart() {
    const pm = this.prestamosPorMes();
    const canvas = document.getElementById('chartPrestamos') as HTMLCanvasElement | null;
    if (!canvas || pm.length === 0) return;
    this.chartPrestamos?.destroy();
    this.chartPrestamos = new Chart(canvas, {
      type: 'bar',
      data: {
        labels: pm.map((x) => x.mes),
        datasets: [{
          label: 'Préstamos',
          data: pm.map((x) => x.cantidad),
          backgroundColor: 'rgba(57, 169, 0, 0.75)',
          borderColor: '#39a900',
          borderWidth: 1,
          borderRadius: 6,
          borderSkipped: false,
        }],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false },
          tooltip: { callbacks: { label: (ctx) => ` ${ctx.parsed.y} préstamos` } },
        },
        scales: {
          y: { beginAtZero: true, ticks: { stepSize: 1, color: '#94a3b8' }, grid: { color: '#f1f5f9' } },
          x: { ticks: { color: '#94a3b8' }, grid: { display: false } },
        },
      },
    });
  }

  private renderDoughnutChart() {
    const ec = this.equiposPorCategoria();
    const canvas = document.getElementById('chartCategoria') as HTMLCanvasElement | null;
    if (!canvas || ec.length === 0) return;
    this.chartCategoria?.destroy();
    const colors = ['#39a900', '#2563eb', '#d97706', '#7c3aed', '#0891b2', '#dc2626', '#65a30d', '#4f46e5'];
    this.chartCategoria = new Chart(canvas, {
      type: 'doughnut',
      data: {
        labels: ec.map((x) => x.categoriaNombre),
        datasets: [{
          data: ec.map((x) => x.cantidad),
          backgroundColor: colors.slice(0, ec.length),
          borderWidth: 2,
          borderColor: '#fff',
        }],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        cutout: '65%',
        plugins: {
          legend: { position: 'right', labels: { font: { size: 12 }, boxWidth: 12, padding: 14, color: '#475569' } },
          tooltip: { callbacks: { label: (ctx) => ` ${ctx.label}: ${ctx.parsed}` } },
        },
      },
    });
  }

  estadoLabel(estado: string): string {
    const m: Record<string, string> = {
      SOLICITADO: 'Solicitado', APROBADO: 'Aprobado', ACTIVO: 'Activo',
      DEVUELTO: 'Devuelto', RECHAZADO: 'Rechazado', EN_MORA: 'En mora',
    };
    return m[estado] ?? estado;
  }
}
