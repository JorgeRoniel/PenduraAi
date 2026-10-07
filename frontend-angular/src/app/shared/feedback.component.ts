import { ChangeDetectionStrategy, Component, input } from '@angular/core';

export type FeedbackTone = 'success' | 'error' | 'info';

@Component({
  selector: 'app-feedback',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <p class="feedback" [class]="'feedback feedback--' + tone()" [attr.role]="tone() === 'error' ? 'alert' : 'status'">
      {{ message() }}
    </p>
  `,
  styles: `
    :host { display: block; }
    .feedback { margin: 0; padding: .8rem 1rem; border-radius: var(--radius-sm); font-size: .88rem; line-height: 1.45; }
    .feedback--success { color: var(--color-success); background: var(--color-success-soft); }
    .feedback--error { color: var(--color-danger-strong); background: rgb(248 37 37 / 10%); }
    .feedback--info { color: var(--color-ink-muted); background: var(--color-primary-soft); }
  `
})
export class FeedbackComponent {
  readonly message = input.required<string>();
  readonly tone = input<FeedbackTone>('info');
}
