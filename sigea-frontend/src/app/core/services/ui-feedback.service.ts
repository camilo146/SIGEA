import { Injectable, signal } from '@angular/core';

export type UiToastTone = 'success' | 'error' | 'warning' | 'info';
export type UiDialogTone = 'success' | 'danger' | 'warning' | 'info';

export interface UiToast {
  id: number;
  tone: UiToastTone;
  title: string;
  message: string;
  duration: number;
}

export interface UiDialogOptions {
  title: string;
  message: string;
  confirmText: string;
  cancelText: string;
  tone: UiDialogTone;
  placeholder?: string;
  defaultValue?: string;
}

export interface UiDialogState extends UiDialogOptions {
  type: 'confirm' | 'prompt';
}

@Injectable({ providedIn: 'root' })
export class UiFeedbackService {
  readonly toasts = signal<UiToast[]>([]);
  readonly dialog = signal<UiDialogState | null>(null);
  readonly promptValue = signal('');

  private nextToastId = 1;
  private dialogResolver: ((value: boolean | string | null) => void) | null = null;

  alert(message: string, title = 'Notificación', tone: UiToastTone = 'info'): Promise<void> {
    this.showToast(tone, message, title);
    return Promise.resolve();
  }

  success(message: string, title = 'Operación completada'): void {
    this.showToast('success', message, title);
  }

  error(message: string, title = 'No se pudo completar'): void {
    this.showToast('error', message, title);
  }

  warning(message: string, title = 'Atención'): void {
    this.showToast('warning', message, title);
  }

  info(message: string, title = 'Información'): void {
    this.showToast('info', message, title);
  }

  confirm(message: string, options: Partial<UiDialogOptions> = {}): Promise<boolean> {
    return new Promise<boolean>((resolve) => {
      this.dialogResolver = (value) => resolve(value === true);
      this.dialog.set({
        type: 'confirm',
        title: options.title ?? 'Confirmar acción',
        message,
        confirmText: options.confirmText ?? 'Aceptar',
        cancelText: options.cancelText ?? 'Cancelar',
        tone: options.tone ?? 'success',
      });
    });
  }

  prompt(message: string, defaultValue = '', options: Partial<UiDialogOptions> = {}): Promise<string | null> {
    return new Promise<string | null>((resolve) => {
      this.dialogResolver = (value) => {
        if (value == null || value === false) {
          resolve(null);
          return;
        }
        resolve(String(value));
      };
      this.promptValue.set(options.defaultValue ?? defaultValue);
      this.dialog.set({
        type: 'prompt',
        title: options.title ?? 'Ingresar información',
        message,
        confirmText: options.confirmText ?? 'Aceptar',
        cancelText: options.cancelText ?? 'Cancelar',
        tone: options.tone ?? 'info',
        placeholder: options.placeholder ?? '',
        defaultValue: options.defaultValue ?? defaultValue,
      });
    });
  }

  setPromptValue(value: string): void {
    this.promptValue.set(value);
  }

  dismissToast(id: number): void {
    this.toasts.update((items) => items.filter((item) => item.id !== id));
  }

  resolveDialog(result: boolean | string | null): void {
    const resolver = this.dialogResolver;
    this.dialogResolver = null;
    this.dialog.set(null);
    this.promptValue.set('');
    resolver?.(result);
  }

  private showToast(tone: UiToastTone, message: string, title: string, duration = 4000): void {
    const id = this.nextToastId++;
    this.toasts.update((items) => [...items, { id, tone, title, message, duration }]);
    window.setTimeout(() => this.dismissToast(id), duration);
  }
}
