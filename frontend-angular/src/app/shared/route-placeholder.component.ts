import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'app-route-placeholder',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <main class="route-placeholder">
      <p>{{ label() }}</p>
    </main>
  `,
  styles: `
    .route-placeholder { min-height: 100vh; display: grid; place-items: center; color: var(--color-ink); }
  `
})
export class RoutePlaceholderComponent {
  readonly label = input('Em construção');
}
