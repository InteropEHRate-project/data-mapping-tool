import {Injectable} from '@angular/core';
import {HttpClient, HttpErrorResponse} from "@angular/common/http";
import {Observable, throwError} from "rxjs";
import {catchError, map} from "rxjs/operators";
import {Dataset} from "./dataset.model";
import {AppConfigEnum, AppConfigService} from "../app-config.service";
import {Filter} from "./filter.model";
import {Dictionary} from "./dictionary.model";
import {Request} from "./request/request.model";
import {TopTagsRequest} from "./request/topTagsRequest.model";
import {DatasetsSearchRequest} from "./request/datasetsSearchRequest.model";

/**
 * A singleton service injected to the ngModel level to be used
 * by the whole application to access the external API.
 * API link is set by config.json or karma at the initialization time.
 *
 * @author Danish
 * @Date 2020/05/23
 */
@Injectable()
export class SearchService {

  constructor(
    private environment: AppConfigService,
    private http: HttpClient
  ) {
  }

  /**
   * get the list of {@link Filter} objects.
   *
   * @param req
   */
  getTags(req: TopTagsRequest): Observable<Dictionary> {
    return this.makeGetRequest(req)
      .pipe(map(response => {
        let result = response['result'];
        let search_facets = result['search_facets'];
        let dictionary: Dictionary = new Dictionary({
          count: result['count'],
          filters: {}
        });
        for (let type in search_facets) {
          let filter = search_facets[type];
          dictionary.filters[filter['title']] = filter['items'].map(item => {
            return Filter.deserialize(filter['title'], item);
          });
        }
        return dictionary;
      }));
  }

  /**
   * get dataSets and filters
   *
   * @return an observable object with the list of {@link Dataset}
   * @param req
   */
  getDataSetsAndFilters(req: DatasetsSearchRequest): Observable<Dictionary> {
    return this.makeGetRequest(req)
      .pipe(map(response => {
        let result = response['result'];
        let search_facets = result['search_facets'];
        let results = result['results'];
        let dictionary: Dictionary = new Dictionary({
          count: result['count'],
          datasets: [],
          filters: {}
        });
        if (search_facets) {
          for (let type in search_facets) {
            let filter = search_facets[type];
            dictionary.filters[filter['title']] = filter['items'].map(item => {
              return Filter.deserialize(filter['title'], item);
            });
          }
        }
        if (results) {
          result['results'].map(item => {
            dictionary.datasets.push(
              Dataset.deserialize(item))
          });
        }
        return dictionary;
      }));
  }

  /**
   * this method accept {@link Request} object as parameter and perform http get request
   * to remote api via backend proxy. its uses HTTPRequestHandler servlet to do so.
   * it append the url with action name and params from {@link Request} object.
   *
   * @param {Request} req
   *
   * @return Observable<Object> need to use subscribe method to access the response.
   */
  makeGetRequest(req: Request): Observable<Object> {
    const action = req.action;
    const params = req.getQueryParameters();
    let baseUrl = `${this.environment.config()[AppConfigEnum.apiUrl]}`;
    let queryUrl = `/api/3/action/`;
    queryUrl += action ? `${action}` : ``;
    queryUrl += params ? `?${params}` : ``;
    if (this.environment.config()[AppConfigEnum.apiKey]) {
      queryUrl += `&include_private=true`;
      queryUrl += `&include_drafts=true`;
      return this.http.get(baseUrl.concat(encodeURIComponent(queryUrl).toString()),
        {
          headers: {
            'Authorization': `${this.environment.config()[AppConfigEnum.apiKey]}`
          }
        }
      ).pipe(catchError(this.handleError));
    } else {
      return this.http.get(baseUrl.concat(encodeURIComponent(queryUrl).toString()))
        .pipe(catchError(this.handleError));
    }
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

}
