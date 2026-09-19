import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { API_ENDPOINTS } from '../../core/api/api-endpoints';
import { HomeComponent } from './home.component';

describe('HomeComponent', () => {
  let fixture: ComponentFixture<HomeComponent>;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HomeComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();
    fixture = TestBed.createComponent(HomeComponent);
    http = TestBed.inject(HttpTestingController);
    http.expectOne(API_ENDPOINTS.currentUser).flush(null, { status: 401, statusText: 'Unauthorized' });
  });

  afterEach(() => {
    http.verify();
  });

  it('renders the paginated debt results returned by the API', () => {
    fixture.detectChanges();
    const request = http.expectOne((item) => item.url === API_ENDPOINTS.debt);
    request.flush({
      content: [{ id: 1, cliente: 'João', valor: 40, createdAt: '2026-01-01T10:00:00' }],
      totalElements: 1,
      totalPages: 1
    });
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('João');
    expect(fixture.nativeElement.textContent).toContain('R$');
  });
});
