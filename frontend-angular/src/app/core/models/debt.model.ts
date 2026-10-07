export interface Debt {
  id: number;
  cliente: string;
  valor: number;
  createdAt: string;
}

export interface CreateDebtPayload {
  cliente: string;
  valor: number;
}

export interface UpdateDebtPayload {
  novoValor: number;
}

export interface DebtMutationResponse {
  message: string;
}
