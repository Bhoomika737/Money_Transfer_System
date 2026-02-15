import { Routes } from '@angular/router';
import { AuthGuard } from './auth.guard';

export const routes: Routes = [
  { 
    path: '', 
    redirectTo: '/login',   // ✅ redirect to login instead of dashboard
    pathMatch: 'full' 
  },
  { 
    path: 'login', 
    loadComponent: () => import('./components/login/login.component').then(m => m.LoginComponent)
  },
  { 
    path: 'dashboard', 
    loadComponent: () => import('./components/dashboard/dashboard.component').then(m => m.DashboardComponent),
    canActivate: [AuthGuard]   // ✅ protected
  },
  { 
    path: 'transfer', 
    loadComponent: () => import('./components/transfer/transfer.component').then(m => m.TransferComponent),
    canActivate: [AuthGuard]   // ✅ protected
  },
  { 
    path: 'history', 
    loadComponent: () => import('./components/history/history.component').then(m => m.HistoryComponent),
    canActivate: [AuthGuard]   // ✅ protected
  },
  { 
    path: 'profile', 
    loadComponent: () => import('./components/profile/profile.component').then(m => m.ProfileComponent),
    canActivate: [AuthGuard]   // ✅ protected
  },
  { 
    path: '**', 
    redirectTo: '/login'   // ✅ fallback to login
  }
];
