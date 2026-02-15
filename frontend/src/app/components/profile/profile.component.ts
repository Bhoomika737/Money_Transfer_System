import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { AccountService } from '../../services/account.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatDividerModule,
    MatSnackBarModule
  ],
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.css']
})
export class ProfileComponent implements OnInit {
  holderName = '';
  accountNumber = '';
  balance = 0;
  isLoading = true;
  isEditing = false;
  isSaving = false;
  isChangingPassword = false;
  isPasswordSaving = false;
  hideCurrentPassword = true;
  hideNewPassword = true;
  hideConfirmPassword = true;

  // Edit fields
  editName = '';
  originalName = '';

  // Password form
  passwordForm!: FormGroup;

  constructor(
    private accountService: AccountService,
    private authService: AuthService,
    private router: Router,
    private snackBar: MatSnackBar,
    private fb: FormBuilder
  ) {}

  ngOnInit(): void {
    this.loadAccountData();
    this.initializePasswordForm();
  }

  initializePasswordForm(): void {
    this.passwordForm = this.fb.group({
      currentPassword: ['', [Validators.required]],
      newPassword: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', [Validators.required]]
    });
  }

  loadAccountData(): void {
    const accountId = this.authService.getAccountId() ?? '';
    if (accountId) {
      this.accountService.getAccount(accountId).subscribe({
        next: acc => {
          this.holderName = acc.holderName;
          this.accountNumber = acc.accountNumber ?? accountId;
          this.balance = acc.balance;
          this.editName = acc.holderName;
          this.originalName = acc.holderName;
          this.isLoading = false;
        },
        error: () => {
          this.snackBar.open('Failed to load account data', 'X', { duration: 3000 });
          this.isLoading = false;
        }
      });
    } else {
      this.isLoading = false;
    }
  }

  toggleEdit(): void {
    if (this.isEditing) {
      // Cancel edit
      this.editName = this.originalName;
    }
    this.isEditing = !this.isEditing;
  }

  saveProfile(): void {
    if (!this.editName.trim()) {
      this.snackBar.open('Name cannot be empty', 'X', { duration: 2000 });
      return;
    }

    if (this.editName === this.originalName) {
      this.snackBar.open('No changes to save', 'X', { duration: 2000 });
      return;
    }

    this.isSaving = true;
    // Note: Backend update would go here
    // For now, just show a success message and update local state
    setTimeout(() => {
      this.holderName = this.editName;
      this.originalName = this.editName;
      this.isEditing = false;
      this.isSaving = false;
      this.snackBar.open('Profile updated successfully!', 'X', { duration: 2000 });
    }, 1000);
  }

  togglePasswordChange(): void {
    if (this.isChangingPassword) {
      this.passwordForm.reset();
    }
    this.isChangingPassword = !this.isChangingPassword;
  }

  changePassword(): void {
    if (this.passwordForm.invalid) {
      this.snackBar.open('Please fill all fields correctly', 'X', { duration: 2000 });
      return;
    }

    const { currentPassword, newPassword, confirmPassword } = this.passwordForm.value;

    if (newPassword !== confirmPassword) {
      this.snackBar.open('New passwords do not match', 'X', { duration: 2000 });
      return;
    }

    if (newPassword.length < 6) {
      this.snackBar.open('Password must be at least 6 characters', 'X', { duration: 2000 });
      return;
    }

    this.isPasswordSaving = true;
    const accountId = this.authService.getAccountId() ?? '';

    this.accountService.changePassword(accountId, currentPassword, newPassword).subscribe({
      next: () => {
        this.isPasswordSaving = false;
        this.isChangingPassword = false;
        this.passwordForm.reset();
        this.snackBar.open('Password changed successfully!', 'X', { duration: 2000 });
      },
      error: (err) => {
        this.isPasswordSaving = false;
        const errorMsg = err.error?.message || 'Failed to change password';
        this.snackBar.open(errorMsg, 'X', { duration: 3000 });
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/dashboard']);
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
