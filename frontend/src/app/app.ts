import { Component, OnInit, signal } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';

@Component({
  imports: [RouterLink, RouterOutlet],
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App implements OnInit {
  protected readonly backendStatus = signal('verificando…');

  async ngOnInit(): Promise<void> {
    try {
      const response = await fetch('/api/health');
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      const health: { status: string } = await response.json();
      this.backendStatus.set(health.status === 'ok' ? 'conectado' : 'indisponível');
    } catch {
      this.backendStatus.set('indisponível');
    }
  }
}
