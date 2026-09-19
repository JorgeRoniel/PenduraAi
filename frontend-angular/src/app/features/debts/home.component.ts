import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize, Subscription } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { CreateDebtPayload, Debt } from '../../core/models/debt.model';
import { DebtService } from '../../core/services/debt.service';
import { AppModalComponent } from '../../shared/app-modal.component';
import { FeedbackComponent } from '../../shared/feedback.component';
import { DebtCardComponent } from './debt-card.component';
import { NavbarComponent } from '../shell/navbar.component';

type ModalName = 'create' | 'edit' | 'settle' | null;

@Component({
  selector: 'app-home',
  imports: [CurrencyPipe, ReactiveFormsModule, AppModalComponent, FeedbackComponent, DebtCardComponent, NavbarComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './home.component.html',
  styleUrl: './home.component.css'
})
export class HomeComponent {
  private searchSubscription?: Subscription;
  private readonly formBuilder = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly debtService = inject(DebtService);
  private readonly router = inject(Router);

  readonly user = this.auth.user;
  readonly debts = signal<Debt[]>([]);
  readonly isLoading = signal(false);
  readonly isSubmitting = signal(false);
  readonly errorMessage = signal('');
  readonly successMessage = signal('');
  readonly modal = signal<ModalName>(null);
  readonly selectedDebt = signal<Debt | null>(null);

  readonly searchForm = this.formBuilder.nonNullable.group({
    cliente: ['']
  });

  readonly createForm = this.formBuilder.nonNullable.group({
    cliente: ['', [Validators.required]],
    valor: [0, [Validators.required, Validators.min(0.01)]]
  });

  readonly editForm = this.formBuilder.nonNullable.group({
    novoValor: [0, [Validators.required, Validators.min(0.01)]]
  });

  constructor() {
    this.searchDebts();
  }

  searchDebts(): void {
    this.searchSubscription?.unsubscribe();
    this.isLoading.set(true);
    this.errorMessage.set('');
    const cliente = this.searchForm.controls.cliente.value.trim();

    this.searchSubscription = this.debtService.search({ cliente, page: 0, size: 10, sort: 'cliente' }).pipe(
      finalize(() => this.isLoading.set(false))
    ).subscribe({
      next: (page) => this.debts.set(page.content),
      error: (error: unknown) => this.errorMessage.set(
        getDebtErrorMessage(error, 'Não foi possível buscar as dívidas. Tente novamente.')
      )
    });
  }

  openCreateModal(): void {
    this.clearFeedback();
    this.createForm.reset({ cliente: '', valor: 0 });
    this.modal.set('create');
  }

  openEditModal(debt: Debt): void {
    this.clearFeedback();
    this.selectedDebt.set(debt);
    this.editForm.reset({ novoValor: debt.valor });
    this.modal.set('edit');
  }

  openSettleModal(debt: Debt): void {
    this.clearFeedback();
    this.selectedDebt.set(debt);
    this.modal.set('settle');
  }

  closeModal(): void {
    if (this.isSubmitting()) return;
    this.modal.set(null);
    this.selectedDebt.set(null);
  }

  createDebt(): void {
    if (this.createForm.invalid) {
      this.createForm.markAllAsTouched();
      return;
    }

    const payload: CreateDebtPayload = this.createForm.getRawValue();
    this.runMutation(() => this.debtService.create(payload), 'Dívida cadastrada com sucesso!', () => {
      this.createForm.reset({ cliente: '', valor: 0 });
      this.closeModal();
      this.searchDebts();
    });
  }

  updateDebt(): void {
    const debt = this.selectedDebt();
    if (!debt || this.editForm.invalid) {
      this.editForm.markAllAsTouched();
      return;
    }

    const novoValor = this.editForm.controls.novoValor.value;
    this.runMutation(() => this.debtService.update(debt.id, { novoValor }), 'Valor atualizado com sucesso!', () => {
      this.debts.update((items) => items.map((item) => item.id === debt.id ? { ...item, valor: novoValor } : item));
      this.closeModal();
    });
  }

  settleDebt(): void {
    const debt = this.selectedDebt();
    if (!debt) return;

    this.runMutation(() => this.debtService.settle(debt.id), 'Dívida quitada com sucesso!', () => {
      this.debts.update((items) => items.filter((item) => item.id !== debt.id));
      this.closeModal();
    });
  }

  logout(): void {
    this.auth.logout().subscribe();
    void this.router.navigateByUrl('/login');
  }

  private runMutation(request: () => ReturnType<DebtService['create']>, success: string, afterSuccess: () => void): void {
    this.isSubmitting.set(true);
    this.clearFeedback();
    request().pipe(finalize(() => this.isSubmitting.set(false))).subscribe({
      next: () => {
        this.successMessage.set(success);
        this.isSubmitting.set(false);
        afterSuccess();
      },
      error: (error: unknown) => this.errorMessage.set(
        getDebtErrorMessage(error, 'Não foi possível concluir a operação. Tente novamente.')
      )
    });
  }

  private clearFeedback(): void {
    this.errorMessage.set('');
    this.successMessage.set('');
  }
}

function getDebtErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof HttpErrorResponse) {
    if (typeof error.error === 'string' && error.error.trim()) return error.error;
    if (isMessageBody(error.error) && error.error.message) return error.error.message;
  }
  return fallback;
}

function isMessageBody(value: unknown): value is { message: string } {
  return typeof value === 'object' && value !== null
    && 'message' in value && typeof value.message === 'string';
}
