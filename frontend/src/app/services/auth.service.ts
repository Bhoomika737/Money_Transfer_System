
import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { isPlatformBrowser } from '@angular/common';

@Injectable({ 
  providedIn: 'root' 
})
export class AuthService {
  private apiUrl = 'http://localhost:8080/api/accounts'; 
  private isBrowser: boolean;

  constructor(private http: HttpClient, @Inject(PLATFORM_ID) private platformId: Object) {
    this.isBrowser = isPlatformBrowser(this.platformId);
  }

  login(accountId: string, password: string) {
    return this.http.post<any>(`${this.apiUrl}/login`, { accountId, password });
  }

  logout() {
    if (this.isBrowser) {
      localStorage.removeItem('authToken');
      localStorage.removeItem('accountId');
    }
  }

  isAuthenticated(): boolean {
    return this.isBrowser && !!localStorage.getItem('authToken');
  }

  getToken(): string | null {
    return this.isBrowser ? (localStorage.getItem('authToken') || null) : null;
  }

  getAccountId(): string | null {
    return this.isBrowser ? (localStorage.getItem('accountId') || null) : null;
  }
}

