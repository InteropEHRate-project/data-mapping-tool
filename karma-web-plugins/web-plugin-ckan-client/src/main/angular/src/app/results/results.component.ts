import {Component, Input, OnChanges, OnInit, SimpleChanges} from '@angular/core';
import {Request} from "../common/request/request.model";
import {DatasetsSearchRequest} from "../common/request/datasetsSearchRequest.model";
import {Filter} from "../common/filter.model";
import {DatasetsComponent} from "./datasets/datasets.component";

@Component({
  selector: 'app-results',
  templateUrl: './results.component.html',
  styleUrls: ['./results.component.css'],
})
export class ResultsComponent implements OnInit, OnChanges {
  /**
   * boolean used to listen for changes from home component.
   * On change load complete list of datasets.
   */
  @Input() isLoadAllDatasets: Boolean;
  @Input() filterByTag: Filter;
  @Input() freeTextSearch: string;
  /**
   * variable used by datasets component for API get request.
   * on each modification to this variable datasets component refresh the results by sending a new API request.
   */
  request: Request;
  /**
   * variable populated by datasets component with the list of new filters attached with the outcome of API GET request.
   * variable is then used by this component to refresh the list of filters for datasets displayed.
   */
  allFilters: { [key: string]: Filter[] };
  /**
   * variable used by template to display selected filters. should only be modifies once the result is loaded successfully.
   */
  selectedFilters: Filter[];
  /**
   * used to display waiting image on the screen
   */
  showSpinner: boolean;
  /**
   * variable used to display the query strin on the view in case of successful GET request.
   */
  displayFreeTextSearch: string;

  constructor() {
  }

  /**
   * On component initialization load datasets filter by tag passed as query parameters.
   * In case of no parameter load all datasets.
   */
  ngOnInit(): void {
    this.allFilters = {};
    this.selectedFilters = [];
  }

  /**
   * Function used to listen changes coming from home component.
   * In this case it is triggered when user ask to load all datasets with no filter
   * for example when Datasets button is pressed.
   * @param changes
   */
  ngOnChanges(changes: SimpleChanges) {
    for (const propName in changes) {
      if (changes.hasOwnProperty(propName)) {
        switch (propName) {
          case 'isLoadAllDatasets': {
            if (changes[propName].currentValue) this.loadAllDatasets();
            break;
          }
          case 'filterByTag': {
            if (changes[propName].currentValue) this.onFilterSelected(changes[propName].currentValue);
            break;
          }
          case 'freeTextSearch': {
            if (changes[propName].currentValue) this.sendNewRequest(null, changes[propName].currentValue);
          }
        }
      }
    }
  }

  /**
   * method used to load all datasets with no filters.
   */
  loadAllDatasets() {
    this.sendNewRequest(
      null,
      null
    );
  }

  /**
   * method used by results template as a handler for filter selection.
   * @param filter
   */
  onFilterSelected(filter: Filter) {
    let selectedFilters: Filter[] = [];
    if (this.selectedFilters && this.selectedFilters.length) { // if already has selected filters
      selectedFilters = this.selectedFilters;
    }
    selectedFilters.push(filter);
    this.sendNewRequest(selectedFilters, this.displayFreeTextSearch);
  }

  /**
   * method used by results template as a handler for filter removal.
   * @param tobeRemovedTag
   */
  onFilterRemove(tobeRemovedTag: Filter) {
    if (this.request instanceof DatasetsSearchRequest) {
      this.request.selectedFilters = this.request.selectedFilters.filter(obj => obj.name !== tobeRemovedTag.name);
      if (this.request.selectedFilters.length == 0) { //if no filter is applied anymore.
        if (this.displayFreeTextSearch) {
          this.onSearch(this.displayFreeTextSearch);
        } else {
          this.loadAllDatasets();
        }
      } else { // has selected filters
        this.sendNewRequest(this.request.selectedFilters, this.displayFreeTextSearch);
      }
    }
  }

  /**
   * handler method when dataset component completes the loading
   *
   * @param filters
   */
  onResultLoaded(filters: { [key: string]: Filter[] }) {
    if (filters && this.request instanceof DatasetsSearchRequest) {
      this.selectedFilters = this.request.selectedFilters;
      this.displayFreeTextSearch = this.request.freeTextSearch;
      for (let title in filters) {
        filters[title].forEach(filter => {
          if (this.selectedFilters && this.selectedFilters.length) {
            this.selectedFilters.forEach(selectedFilter => {
              if (selectedFilter.type === title) {
                if (selectedFilter.isEqual(filter)) {
                  filter.activated = true;
                }
              }
            });
          }
        });
        filters[title].sort((a, b) => (a.activated === true ? -1 : a.count > b.count ? -1 : 1));
      }
      this.showSpinner = false;
      this.allFilters = filters;
    }
  }

  /**
   * send a new free text search request while discard the existing results.
   * @param freeSearchText
   */
  onSearch(freeSearchText: string) {
    if (freeSearchText) {
      this.sendNewRequest(null, freeSearchText);
    }
  }

  onFreeTextRemove() {
    if (this.selectedFilters && this.selectedFilters.length) { // if already has selected filters
      this.sendNewRequest(this.selectedFilters, '');
    } else {
      this.loadAllDatasets();
    }
  }

  private sendNewRequest(selectedFilter: Filter[], freeText: string) {
    this.showSpinner = true;
    this.allFilters = {};
    this.request = new DatasetsSearchRequest({
      selectedFilters: selectedFilter ?? null,
      freeTextSearch: freeText ?? null,
      row: DatasetsComponent.DATASETS_PER_PAGE ?? null
    });
  }

}
