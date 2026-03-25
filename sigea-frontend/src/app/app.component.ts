import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { UiFeedbackHostComponent } from './shared/ui-feedback-host.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, UiFeedbackHostComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent {
  title = 'sigea-frontend';
}
