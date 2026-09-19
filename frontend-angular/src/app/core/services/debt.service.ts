import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_ENDPOINTS } from '../api/api-endpoints';
import { CreateDebtPayload, Debt, UpdateDebtPayload } from '../models/debt.model';
import { Page } from '../models/page.model';

export interface DebtQuery {
  cliente?: string;
  page?: number;
  size?: number;
  sort?: string;
}

@Injectable({ providedIn: 'root' })
export class DebtService {
  constructor(private readonly http: HttpClient) {}

  search(query: DebtQuery = {}): Observable<Page<Debt>> {
    let params = new HttpParams();
    if (query.cliente) params = params.set('cliente', query.cliente);
    if (query.page !== undefined) params = params.set('page', query.page);
    if (query.size !== undefined) params = params.set('size', query.size);
    if (query.sort) params = params.set('sort', query.sort);

    return this.http.get<Page<Debt>>(API_ENDPOINTS.debt, { params });
  }

  create(payload: CreateDebtPayload): Observable<string> {
    return this.http.post(API_ENDPOINTS.debt, payload, { responseType: 'text' });
  }

  update(id: number, payload: UpdateDebtPayload): Observable<string> {
    return this.http.put(`${API_ENDPOINTS.debt}/${id}/update`, payload, { responseType: 'text' });
  }

  settle(id: number): Observable<string> {
    return this.http.delete(`${API_ENDPOINTS.debt}/${id}/quitar`, { responseType: 'text' });
  }
}
