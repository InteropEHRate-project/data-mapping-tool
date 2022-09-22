import {async, ComponentFixture, TestBed} from '@angular/core/testing';

import {SearchComponent} from './search.component';
import {ReactiveFormsModule} from "@angular/forms";

describe('SearchComponent', () => {
  let component: SearchComponent;
  let fixture: ComponentFixture<SearchComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [
        ReactiveFormsModule
      ],
      declarations: [SearchComponent]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SearchComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  /**
   * testing Model-Driven Forms
   */
  it('testing the search form', () => {
    component.searchForm.controls['searchQuery'].setValue("free text");

    let text: string = '';
    // Subscribe to the Observable and store the text in a local variable.
    component.textForSearch.subscribe((value) => text = value);

    // Trigger the on search function
    component.onSearch();

    // text for result
    expect(text).toBe("free text");
  });

});
