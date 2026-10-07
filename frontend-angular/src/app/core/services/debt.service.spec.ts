import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_ENDPOINTS } from '../api/api-endpoints';
import { DebtService } from './debt.service';

describe('DebtService', () => {
  let service: DebtService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [DebtService, provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(DebtService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('builds a typed paginated search request', () => {
    service.search({ cliente: 'João', page: 1, size: 10, sort: 'cliente' }).subscribe((page) => {
      expect(page.content[0].cliente).toBe('João');
      expect(page.totalElements).toBe(1);
    });

    const request = http.expectOne((item) => item.url === API_ENDPOINTS.debt);
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('cliente')).toBe('João');
    expect(request.request.params.get('page')).toBe('1');
    request.flush({ content: [{ id: 3, cliente: 'João', valor: 25, createdAt: '2026-01-01T10:00:00' }], totalElements: 1 });
  });

  it('uses the backend endpoints for update and settlement', () => {
    service.update(3, { novoValor: 50 }).subscribe();
    const update = http.expectOne(`${API_ENDPOINTS.debt}/3/update`);
    expect(update.request.body).toEqual({ novoValor: 50 });
    update.flush('Valor atualizado com sucesso!');

    service.settle(3).subscribe();
    const settle = http.expectOne(`${API_ENDPOINTS.debt}/3/quitar`);
    expect(settle.request.method).toBe('DELETE');
    settle.flush('Dívida quitada com sucesso!');
  });
});
