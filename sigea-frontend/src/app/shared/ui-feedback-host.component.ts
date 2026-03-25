import { CommonModule } from '@angular/common';
import { Component, ElementRef, HostListener, ViewChild, effect, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { UiFeedbackService } from '../core/services/ui-feedback.service';

@Component({
  selector: 'app-ui-feedback-host',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="toast-container" aria-live="polite" aria-atomic="true">
      @for (toast of ui.toasts(); track toast.id) {
        <div class="toast" [class]="'toast toast-' + toast.tone" role="status">
          <div class="toast-icon" aria-hidden="true">
            <i class="fas" [ngClass]="toastIcon(toast.tone)"></i>
          </div>
          <div class="toast-body">
            <div class="toast-title-row">
              <strong class="toast-title">{{ toast.title }}</strong>
              <button type="button" class="toast-close" (click)="ui.dismissToast(toast.id)" aria-label="Cerrar notificación">
                <i class="fas fa-times"></i>
              </button>
            </div>
            <p class="toast-msg">{{ toast.message }}</p>
            <div class="toast-progress"><span [style.animationDuration.ms]="toast.duration"></span></div>
          </div>
        </div>
      }
    </div>

    @if (ui.dialog(); as dialog) {
      <div class="app-dialog-backdrop" (click)="cancelDialog()">
        <div
          #dialogPanel
          class="app-dialog"
          [class]="'app-dialog app-dialog-' + dialog.tone"
          role="dialog"
          aria-modal="true"
          aria-labelledby="app-dialog-title"
          aria-describedby="app-dialog-message"
          tabindex="-1"
          (click)="$event.stopPropagation()"
        >
          <div class="app-dialog-header">
            <div class="app-dialog-icon" aria-hidden="true">
              <i class="fas" [ngClass]="dialogIcon(dialog.tone, dialog.type)"></i>
            </div>
            <div>
              <h3 id="app-dialog-title">{{ dialog.title }}</h3>
              <p id="app-dialog-message">{{ dialog.message }}</p>
            </div>
          </div>

          @if (dialog.type === 'prompt') {
            <div class="app-dialog-field">
              <label class="sr-only" for="app-dialog-input">Ingresar valor</label>
              <input
                #promptInput
                id="app-dialog-input"
                type="text"
                [value]="ui.promptValue()"
                [placeholder]="dialog.placeholder || ''"
                (input)="ui.setPromptValue(promptInput.value)"
                (keydown.enter)="submitPrompt($event)"
              />
            </div>
          }

          <div class="app-dialog-actions">
            <button type="button" class="btn btn-outline" (click)="cancelDialog()">{{ dialog.cancelText }}</button>
            <button #primaryAction type="button" class="btn" [class]="primaryButtonClass(dialog.tone)" (click)="confirmDialog()">
              {{ dialog.confirmText }}
            </button>
          </div>
        </div>
      </div>
    }
  `,
})
export class UiFeedbackHostComponent {
  readonly ui = inject(UiFeedbackService);

  @ViewChild('dialogPanel') private dialogPanel?: ElementRef<HTMLDivElement>;
  @ViewChild('primaryAction') private primaryAction?: ElementRef<HTMLButtonElement>;
  @ViewChild('promptInput') private promptInput?: ElementRef<HTMLInputElement>;

  constructor() {
    effect(() => {
      if (this.ui.dialog()) {
        document.body.style.overflow = 'hidden';
        queueMicrotask(() => {
          const prompt = this.promptInput?.nativeElement;
          const primary = this.primaryAction?.nativeElement;
          (prompt ?? primary ?? this.dialogPanel?.nativeElement)?.focus();
        });
        return;
      }
      document.body.style.overflow = '';
    });
  }

  @HostListener('document:keydown', ['$event'])
  onKeydown(event: KeyboardEvent): void {
    const dialog = this.ui.dialog();
    if (!dialog) return;

    if (event.key === 'Escape') {
      event.preventDefault();
      this.cancelDialog();
      return;
    }

    if (event.key !== 'Tab') return;
    const focusable = this.getFocusableElements();
    if (!focusable.length) return;

    const first = focusable[0];
    const last = focusable[focusable.length - 1];
    const active = document.activeElement as HTMLElement | null;

    if (event.shiftKey && active === first) {
      event.preventDefault();
      last.focus();
    } else if (!event.shiftKey && active === last) {
      event.preventDefault();
      first.focus();
    }
  }

  toastIcon(tone: string): string {
    const map: Record<string, string> = {
      success: 'fa-check-circle',
      error: 'fa-circle-exclamation',
      warning: 'fa-triangle-exclamation',
      info: 'fa-circle-info',
    };
    return map[tone] ?? 'fa-circle-info';
  }

  dialogIcon(tone: string, type: 'confirm' | 'prompt'): string {
    if (type === 'prompt') return 'fa-pen-to-square';
    const map: Record<string, string> = {
      success: 'fa-circle-check',
      danger: 'fa-triangle-exclamation',
      warning: 'fa-circle-exclamation',
      info: 'fa-circle-info',
    };
    return map[tone] ?? 'fa-circle-question';
  }

  primaryButtonClass(tone: string): string {
    return tone === 'danger' ? 'btn btn-danger' : tone === 'warning' ? 'btn btn-warning' : 'btn btn-primary';
  }

  cancelDialog(): void {
    const dialog = this.ui.dialog();
    if (!dialog) return;
    this.ui.resolveDialog(dialog.type === 'prompt' ? null : false);
  }

  confirmDialog(): void {
    const dialog = this.ui.dialog();
    if (!dialog) return;
    this.ui.resolveDialog(dialog.type === 'prompt' ? this.ui.promptValue() : true);
  }

  submitPrompt(event: Event): void {
    event.preventDefault();
    this.confirmDialog();
  }

  private getFocusableElements(): HTMLElement[] {
    const panel = this.dialogPanel?.nativeElement;
    if (!panel) return [];

    return Array.from(
      panel.querySelectorAll<HTMLElement>('button:not([disabled]), input:not([disabled]), textarea:not([disabled]), select:not([disabled]), [tabindex]:not([tabindex="-1"])')
    );
  }
}
