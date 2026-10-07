import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { ReactiveFormsModule, Validators, FormBuilder } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthLayoutComponent } from './auth-layout.component';
import { getLoginErrorMessage } from './auth-feedback';
import { AuthService } from '../../core/services/auth.service';
import { FeedbackComponent } from '../../shared/feedback.component';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, AuthLayoutComponent, FeedbackComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './login.component.html',
  styleUrl: './auth-form.css'
})
export class LoginComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly submitted = signal(false);
  readonly isSubmitting = signal(false);
  readonly errorMessage = signal('');

  readonly form = this.formBuilder.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    senha: ['', [Validators.required]]
  });

  submit(): void {
    this.submitted.set(true);
    this.errorMessage.set('');

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.auth.login(this.form.getRawValue()).pipe(
      finalize(() => this.isSubmitting.set(false))
    ).subscribe({
      next: () => void this.router.navigateByUrl('/'),
      error: (error: unknown) => this.errorMessage.set(
        getLoginErrorMessage(error, 'Não foi possível entrar. Tente novamente.')
      )
    });
  }
}
