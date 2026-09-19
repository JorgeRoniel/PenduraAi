import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { Debt } from '../../core/models/debt.model';

@Component({
  selector: 'app-debt-card',
  imports: [CurrencyPipe, DatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './debt-card.component.html',
  styleUrl: './debt-card.component.css'
})
export class DebtCardComponent {
  readonly debt = input.required<Debt>();
  readonly editRequested = output<Debt>();
  readonly settleRequested = output<Debt>();
}
