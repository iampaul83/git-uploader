import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface PatStatus {
  configured: boolean;
  maskedPat: string | null;
}

export interface CreateRepoRequest {
  url: string;
  branch?: string | null;
}

export interface RepoSummary {
  id: string;
  project: string;
  repository: string;
  branch: string;
  url: string;
  path: string;
  yearBranchPath: string;
}

export interface CommitResponse {
  committed: boolean;
  message: string;
}

@Injectable({ providedIn: 'root' })
export class GitService {
  private readonly baseUrl = '/api';

  constructor(private readonly http: HttpClient) {}

  getPatStatus(): Observable<PatStatus> {
    return this.http.get<PatStatus>(`${this.baseUrl}/pat`);
  }

  updatePat(pat: string): Observable<PatStatus> {
    return this.http.post<PatStatus>(`${this.baseUrl}/pat`, { pat });
  }

  listRepos(): Observable<RepoSummary[]> {
    return this.http.get<RepoSummary[]>(`${this.baseUrl}/repos`);
  }

  addRepo(request: CreateRepoRequest): Observable<RepoSummary> {
    return this.http.post<RepoSummary>(`${this.baseUrl}/repos`, request);
  }

  commitAndPush(id: string, message: string): Observable<CommitResponse> {
    return this.http.post<CommitResponse>(`${this.baseUrl}/repos/${encodeURIComponent(id)}/commit-and-push`, {
      message
    });
  }
}
