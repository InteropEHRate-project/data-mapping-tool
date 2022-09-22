import {Component, EventEmitter, OnInit, Output} from '@angular/core';
import {FormControl, FormGroup} from "@angular/forms";

@Component({
  selector: 'app-search',
  templateUrl: './search.component.html',
  styleUrls: ['./search.component.css']
})
export class SearchComponent implements OnInit {

  @Output() textForSearch = new EventEmitter<string>();
  /**
   * angular search form used by the home template.
   */
  searchForm = new FormGroup({
    searchQuery: new FormControl('')
  });

  constructor() {
  }

  ngOnInit(): void {
  }

  /**
   * handler for free text search
   */
  onSearch() {
    this.textForSearch.emit(this.searchForm.value.searchQuery);
  }

}
