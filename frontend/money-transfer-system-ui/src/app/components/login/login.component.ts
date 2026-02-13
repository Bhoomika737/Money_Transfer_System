import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatCheckboxModule,
    MatTooltipModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit {
  accountId = '';
  password = '';
  errorMessage = '';

  captchaCode = '';
  captchaInput = '';
  rememberMe = false;
  hidePassword = true;
  isLoading = false;

  
  accountIdPattern = /^[A-Za-z]+\d+$/; 
  
  passwordPattern = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{5,}$/;

  constructor(private authService: AuthService, private router: Router) {
    this.generateCaptcha();
  }

  ngOnInit() {
    if (this.authService.isAuthenticated()) {
      this.router.navigate(['/dashboard']);
    }
  }

  generateCaptcha() {
    const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789';
    this.captchaCode = '';
    for (let i = 0; i < 6; i++) {
      this.captchaCode += chars.charAt(Math.floor(Math.random() * chars.length));
    }
  }

  togglePasswordVisibility() {
    this.hidePassword = !this.hidePassword;
  }

  login() {
    // ✅ Prevent login if another user is already logged in
    if (this.authService.isAuthenticated()) {
      this.errorMessage = 'Another user is already logged in. Please logout first.';
      return;
    }

    if (!this.accountId || !this.password) {
      this.errorMessage = 'Username and password are required';
      return;
    }

    if (!this.accountIdPattern.test(this.accountId)) {
      this.errorMessage = 'Account ID must start with letters and end with numbers (e.g. ACC001)';
      return;
    }

    if (!this.passwordPattern.test(this.password)) {
      this.errorMessage = 'Password must include letters, numbers, a special symbol, and be at least 5 characters long';
      return;
    }

    if (this.captchaInput !== this.captchaCode) {
      this.errorMessage = 'Captcha does not match. Please try again.';
      this.generateCaptcha();
      this.captchaInput = '';
      return;
    }

    this.isLoading = true;
    this.authService.login(this.accountId, this.password).subscribe({
      next: res => {
        localStorage.setItem('authToken', res.token || btoa(`${this.accountId}:${this.password}`));
        localStorage.setItem('accountId', res.accountId);

        this.errorMessage = '';
        this.isLoading = false;
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Invalid credentials. Please try again.';
        this.isLoading = false;
      }
    });
  }
}
