import {Component, OnInit} from '@angular/core';
import {SearchService} from "../common/search.service";
import {Filter} from "../common/filter.model";
import {Dictionary} from "../common/dictionary.model";
import {TopTagsRequest} from "../common/request/topTagsRequest.model";
import {FiltersEnum} from "../common/api.enum";

/**
 * component to handle the home page view.
 *
 * @author Danish
 * @date 2020/05/23
 */
@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent implements OnInit {
  /**
   * result component listen to changes to this variable to load/reload All datasets
   */
  isLoadAllDatasets: Boolean;
  filterByTag: Filter;
  freeTextSearch: string;
  /**
   * boolean attached with home button., to load the results.
   */
  isLoadResults: boolean;
  /**
   * variable used to save top tags from api and display on the screen.
   */
  topTags: Array<Filter> = [];
  /**
   * boolean to show the spinner on the screen
   */
  isShowSpinner: boolean;
  switchCase: number;

  constructor(
    private searchService: SearchService
  ) {
  }

  /**
   * get top 10 tags on initialization.
   */
  ngOnInit(): void {
    this.isLoadResults = false;
    this.isLoadAllDatasets = false;
    this.filterByTag = null;
    this.getTopTags(new TopTagsRequest({
      facet_limit: 10
    }));
  }

  /**
   * get datasets by tag.
   * @param tag
   */
  getDatasetByTag(tag: Filter) {
    this.switchCase = 1;
    this.isLoadResults = true;
    this.filterByTag = tag;
  }

  /**
   * handler for free text search
   */
  onSearch(freeSearchText: string) {
    if (freeSearchText) {
      this.switchCase = 2;
      this.isLoadResults = true;
      this.freeTextSearch = freeSearchText;
    }
  }

  /**
   * tell results component to load all datasets
   */
  datasets() {
    this.switchCase = 3;
    this.isLoadResults = true;
    this.isLoadAllDatasets = Boolean(true);
  }

  organizations() {
    this.switchCase = 4;
    this.isLoadResults = true;
  }

  groups() {
    this.switchCase = 5;
    this.isLoadResults = true;
  }

  /**
   * reinitialise home component
   */
  reloadHomePage() {
    this.ngOnInit();
  }

  /**
   * get Top 10 list of tags
   */
  private getTopTags(req: TopTagsRequest): void {
    this.isShowSpinner = true;
    this.searchService.getTags(req).subscribe(
      (results: Dictionary) => { // on success
        this.topTags = results.filters[FiltersEnum.tags];
      },
      (err: any) => { // on error
        console.log(`Error while getting tags: ${err}`);
        this.isShowSpinner = false;
      },
      () => { // on completion
        this.isShowSpinner = false;
      }
    );
  }
}
