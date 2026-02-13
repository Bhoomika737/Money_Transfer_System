
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';

@Injectable({ 
  providedIn: 'root' 
})
export class AuthService {
  private apiUrl = 'http://localhost:8080/api/accounts'; 
  constructor(private http: HttpClient) {}

  login(accountId: string, password: string) {
    return this.http.post<any>(`${this.apiUrl}/login`, { accountId, password });
  }

  logout() {
    localStorage.removeItem('authToken');
    localStorage.removeItem('accountId');
  }

  isAuthenticated(): boolean {
  return !!localStorage.getItem('authToken');
}

}