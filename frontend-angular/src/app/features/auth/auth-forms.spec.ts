import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { LoginComponent } from './login.component';
import { RegisterComponent } from './register.component';

describe('authentication forms', () => {
  let loginFixture: ComponentFixture<LoginComponent>;
  let registerFixture: ComponentFixture<RegisterComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LoginComponent, RegisterComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();
    loginFixture = TestBed.createComponent(LoginComponent);
    registerFixture = TestBed.createComponent(RegisterComponent);
  });

  it('requires email and password on login', () => {
    const component = loginFixture.componentInstance;
    component.submit();

    expect(component.form.invalid).toBeTrue();
    expect(component.form.controls.email.touched).toBeTrue();
    expect(component.form.controls.senha.touched).toBeTrue();
  });

  it('requires a valid email and six-character password on registration', () => {
    const component = registerFixture.componentInstance;
    component.form.setValue({ nome: 'Ana', email: 'ana', senha: '123' });
    component.submit();

    expect(component.form.invalid).toBeTrue();
    expect(component.form.controls.email.hasError('email')).toBeTrue();
    expect(component.form.controls.senha.hasError('minlength')).toBeTrue();
  });
});
