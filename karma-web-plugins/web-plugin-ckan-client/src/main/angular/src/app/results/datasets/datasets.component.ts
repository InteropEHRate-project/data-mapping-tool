import {Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges} from '@angular/core';
import {Dataset} from "../../common/dataset.model";
import {SearchService} from "../../common/search.service";
import {Request} from "../../common/request/request.model";
import {Filter} from "../../common/filter.model";
import {Dictionary} from "../../common/dictionary.model";
import {AppConfigEnum} from "../../app-config.service";
import {DatasetsSearchRequest} from "../../common/request/datasetsSearchRequest.model";

@Component({
  selector: 'app-datasets',
  templateUrl: './datasets.component.html',
  styleUrls: ['./datasets.component.css']
})
export class DatasetsComponent implements OnInit, OnChanges {
  /**
   * constant used to define datasets to be loaded on a single page
   */
  public static DATASETS_PER_PAGE: number = 10;
  /**
   * variable used to listen for changes coming from results component.
   */
  @Input() request: Request;
  /**
   * Results Component listens for Datasets Component event.
   * On emmit a method mentioned in results template will be called with the emitted value.
   */
  @Output() allFilters = new EventEmitter<{ [key: string]: Filter[] }>();
  /**
   * variable used to display and refresh the results on the page.
   */
  resultDatasets: Array<Dataset>;
  /**
   * variables to control the spinner.
   */
  showSpinner: boolean;
  /**
   * variable for Pagination feature.
   */
  pageSize = DatasetsComponent.DATASETS_PER_PAGE;
  page = 1;
  /**
   * total number of datasets found for a given request.
   */
  numberOfDatasets: number;

  constructor(
    private search: SearchService
  ) {
  }

  ngOnInit(): void {
  }

  /**
   * listen to changes to the variable coming for calling components.
   * In this case only {@link request} variable from results component.
   * @param changes
   */
  ngOnChanges(changes: SimpleChanges) {
    for (const propName in changes) {
      if (changes.hasOwnProperty(propName)) {
        switch (propName) {
          case 'request': {
            if (changes[propName].currentValue) {
              this.numberOfDatasets = 0;
              this.loadNewPage(1);
            }
          }
        }
      }
    }
  }

  /**
   * function used to handle a new page load request.
   * @param currentPage
   */
  public loadNewPage(currentPage: number) {
    this.showSpinner = true;
    this.resultDatasets = [];
    this.request.start = ((+currentPage) - 1) * DatasetsComponent.DATASETS_PER_PAGE;
    this.loadDataSets(currentPage);
  }

  /**
   * Sent the dataset to Karma backend and close the window.
   * @param dataset
   */
  public importCatalog(dataset: Dataset): void {
    if (dataset) {
      localStorage.setItem(AppConfigEnum.datasetId, dataset.id);
      localStorage.setItem(AppConfigEnum.selectedResources, JSON.stringify(dataset.getResourceIds()));
      localStorage.setItem(AppConfigEnum.isDatasetSelected, 'true');
    }
  }

  /**
   * make a remote http request and on success show the datasets on screen.
   * @param currentPage
   */
  private loadDataSets(currentPage: number): void {
    if (this.request instanceof DatasetsSearchRequest) {
      this.search.getDataSetsAndFilters(this.request as DatasetsSearchRequest).subscribe(
        (results: Dictionary) => { // on sucesss
          this.resultDatasets = results.datasets;
          // reset count displayed on result page.
          if (currentPage == 1) this.numberOfDatasets = results.count;
          // send refreshed filters to the results component.
          this.allFilters.emit(results.filters);
        },
        (err: any) => { // on error
          console.log(`Error while getting dataset: ${err}`);
          this.showSpinner = false;
        },
        () => { // on completion
          this.showSpinner = false;
          console.log('COMPLETED: Datasets REQUEST');
        }
      );
    }
  }
}
