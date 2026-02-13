import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class HistoryService {
  private apiEndpoint ="http://localhost:8080/api" ;

  constructor(private http: HttpClient) {}

  fetchTransactionHistory(accountId: string): Observable<any[]> {
  return this.http.get<any[]>(`${this.apiEndpoint}/transfers/${accountId}`);
}

}
