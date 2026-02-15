import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class TransferService {
  private apiUrl = 'http://localhost:8080/api/transfers';

  constructor(private http: HttpClient) {}

  transferFunds(payload: {
    fromAccountId: string;
    toAccountId: string;
    amount: number;
    remarks?: string;
    idempotencyKey: string;
  }): Observable<any> {
    return this.http.post<any>(this.apiUrl, payload, {
      headers: {
        'Idempotency-Key': payload.idempotencyKey
      }
    });
  }
}
