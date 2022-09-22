import {Injectable, NgZone} from '@angular/core';
import {HttpClient, HttpErrorResponse} from "@angular/common/http";
import {Request} from "./request/request.model";
import {Observable, throwError} from "rxjs";
import {catchError} from "rxjs/operators";
import {RunProcessRequest} from "./request/runProcessRequest.model";
import {SseClient} from "angular-sse-client";
import { closeEventSource } from 'angular-sse-client';

@Injectable({
  providedIn: 'root'
})
export class BatchModeBackendService {
  BASEURL: string = "/batch-mode/rest/process";

  constructor(
    private http: HttpClient,
    private zone: NgZone,
    private sseClient: SseClient
  ) {
  }

  makePostRequest(req: Request): Observable<any> {
    const path = req.path;
    let url = this.BASEURL;
    url += path ? `${path}` : ``;
    if (req instanceof RunProcessRequest) {
      return this.http.post(url, req.getFormData(), {
        reportProgress: true,
        observe: 'events',
      });
    }
    return this.http.post(url, req.getFormData());
  }

  makeSeeRequest(req: Request): Observable<any> {
    const path = req.path;
    let url = this.BASEURL;
    url += path ? `${path}` : ``;
    return this.sseClient.get(url);
  }

  makeGetRequest(req: Request): Observable<any> {
    const path = req.path;
    let url = this.BASEURL;
    url += path ? `${path}` : ``;
    return this.http.get(url)
      .pipe(catchError(this.handleError));
  }

  private handleError(error: HttpErrorResponse) {
    if (error.error instanceof ErrorEvent) {
      // A client-side or network error occurred. Handle it accordingly.
      console.error('An error occurred:', error.error.message);
    } else {
      // The backend returned an unsuccessful response code.
      // The response body may contain clues as to what went wrong,
      console.error(
        `Backend returned code ${error.status}, ` +
        `body was: ${error.error}`);
    }
    // return an observable with a user-facing error message
    return throwError(
      'Something bad happened; please try again later.');
  }

  /**
   * close see connection
   */
  closeSseConnection(req: Request): void {
    const path = req.path;
    let url = this.BASEURL;
    url += path ? `${path}` : ``;
    closeEventSource(url);
  }
}
