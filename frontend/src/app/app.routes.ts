import { Routes } from '@angular/router';
import { DocumentsPage } from './documents-page';
import { DocumentPage } from './document-page';

export const routes: Routes = [
  { path: '', component: DocumentsPage },
  { path: 'documents/:id', component: DocumentPage },
  { path: '**', redirectTo: '' },
];
