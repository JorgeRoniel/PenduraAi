import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { AuthLayoutComponent } from './auth-layout.component';
import { getAuthErrorMessage } from './auth-feedback';
import { FeedbackComponent } from '../../shared/feedback.component';

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, AuthLayoutComponent, FeedbackComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './register.component.html',
  styleUrl: './auth-form.css'
})
export class RegisterComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly submitted = signal(false);
  readonly isSubmitting = signal(false);
  readonly errorMessage = signal('');
  readonly successMessage = signal('');

  readonly form = this.formBuilder.nonNullable.group({
    nome: ['', [Validators.required, Validators.minLength(2)]],
    email: ['', [Validators.required, Validators.email]],
    senha: ['', [Validators.required, Validators.minLength(6)]]
  });

  submit(): void {
    this.submitted.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.auth.register(this.form.getRawValue()).pipe(
      finalize(() => this.isSubmitting.set(false))
    ).subscribe({
      next: () => {
        this.successMessage.set('Cadastro realizado! Redirecionando para o login...');
        window.setTimeout(() => void this.router.navigateByUrl('/login'), 900);
      },
      error: (error: unknown) => this.errorMessage.set(
        getAuthErrorMessage(error, 'Não foi possível cadastrar. Verifique os dados informados.')
      )
    });
  }
}
