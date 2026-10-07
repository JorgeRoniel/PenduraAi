import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-auth-layout',
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './auth-layout.component.html',
  styleUrl: './auth-layout.component.css'
})
export class AuthLayoutComponent {
  readonly title = input.required<string>();
  readonly description = input.required<string>();
  readonly alternateText = input.required<string>();
  readonly alternateLabel = input.required<string>();
  readonly alternateLink = input.required<string>();
}
