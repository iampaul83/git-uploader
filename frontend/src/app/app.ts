import { CommonModule, NgFor, NgIf } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatListModule } from '@angular/material/list';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTabsModule } from '@angular/material/tabs';
import { MatToolbarModule } from '@angular/material/toolbar';
import { GitService, PatStatus, RepoSummary } from './git.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule,
    NgIf,
    NgFor,
    ReactiveFormsModule,
    MatToolbarModule,
    MatTabsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatListModule,
    MatIconModule,
    MatProgressBarModule
  ],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  private readonly gitService = inject(GitService);
  private readonly formBuilder = inject(FormBuilder);

  protected readonly patStatus = signal<PatStatus | null>(null);
  protected readonly repos = signal<RepoSummary[]>([]);
  protected readonly patLoading = signal(false);
  protected readonly repoListLoading = signal(false);
  protected readonly addRepoLoading = signal(false);
  protected readonly commitLoading = signal<string | null>(null);
  protected readonly infoMessage = signal<string | null>(null);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly patForm = this.formBuilder.nonNullable.group({
    pat: ['', [Validators.required]]
  });

  protected readonly repoForm = this.formBuilder.nonNullable.group({
    url: ['', [Validators.required]],
    branch: ['']
  });

  private readonly commitControls = new Map<string, FormControl<string>>();

  constructor() {
    this.refresh();
  }

  protected refresh(): void {
    this.loadPatStatus();
    this.loadRepos();
  }

  protected savePat(): void {
    if (this.patForm.invalid) {
      this.patForm.markAllAsTouched();
      return;
    }
    const value = this.patForm.controls.pat.value.trim();
    this.clearMessages();
    this.patLoading.set(true);
    this.gitService.updatePat(value).subscribe({
      next: (status) => {
        this.patStatus.set(status);
        this.infoMessage.set('已更新個人存取權杖。');
        this.patLoading.set(false);
        this.patForm.reset();
      },
      error: (error) => {
        this.handleError(error);
        this.patLoading.set(false);
      }
    });
  }

  protected addRepo(): void {
    if (this.repoForm.invalid) {
      this.repoForm.markAllAsTouched();
      return;
    }
    const payload = this.repoForm.getRawValue();
    this.clearMessages();
    this.addRepoLoading.set(true);
    this.gitService
      .addRepo({
        url: payload.url.trim(),
        branch: payload.branch?.trim() ? payload.branch.trim() : undefined
      })
      .subscribe({
        next: (repo) => {
          this.repos.set([...this.repos(), repo]);
          this.repoForm.reset();
          this.infoMessage.set(`已新增 ${repo.project}/${repo.repository} (${repo.branch})。`);
          this.addRepoLoading.set(false);
        },
        error: (error) => {
          this.handleError(error);
          this.addRepoLoading.set(false);
        }
      });
  }

  protected commitControl(repoId: string): FormControl<string> {
    let control = this.commitControls.get(repoId);
    if (!control) {
      control = this.formBuilder.nonNullable.control('', [Validators.required, Validators.maxLength(200)]);
      this.commitControls.set(repoId, control);
    }
    return control;
  }

  protected commitAndPush(repo: RepoSummary): void {
    const control = this.commitControl(repo.id);
    control.markAsTouched();
    if (control.invalid) {
      return;
    }
    const message = control.value.trim();
    if (!message) {
      control.setErrors({ required: true });
      return;
    }

    this.clearMessages();
    this.commitLoading.set(repo.id);
    this.gitService.commitAndPush(repo.id, message).subscribe({
      next: (response) => {
        this.infoMessage.set(response.message);
        if (response.committed) {
          control.reset();
        }
        this.commitLoading.set(null);
      },
      error: (error) => {
        this.handleError(error);
        this.commitLoading.set(null);
      }
    });
  }

  protected trackById(_: number, repo: RepoSummary): string {
    return repo.id;
  }

  private loadPatStatus(): void {
    this.patLoading.set(true);
    this.gitService.getPatStatus().subscribe({
      next: (status) => {
        this.patStatus.set(status.configured ? status : null);
        this.patLoading.set(false);
      },
      error: (error) => {
        this.handleError(error);
        this.patLoading.set(false);
      }
    });
  }

  private loadRepos(): void {
    this.repoListLoading.set(true);
    this.gitService.listRepos().subscribe({
      next: (repos) => {
        this.repos.set(repos);
        this.repoListLoading.set(false);
      },
      error: (error) => {
        this.handleError(error);
        this.repoListLoading.set(false);
      }
    });
  }

  private handleError(error: unknown): void {
    const message = error instanceof Error ? error.message : this.extractHttpError(error);
    this.errorMessage.set(message || '操作失敗，請稍後再試。');
  }

  private extractHttpError(error: unknown): string | null {
    if (typeof error === 'object' && error !== null && 'error' in error) {
      const payload = (error as { error: unknown }).error as { message?: string } | null;
      if (payload && typeof payload === 'object' && 'message' in payload && typeof payload.message === 'string') {
        return payload.message;
      }
    }
    return null;
  }

  private clearMessages(): void {
    this.infoMessage.set(null);
    this.errorMessage.set(null);
  }
}
