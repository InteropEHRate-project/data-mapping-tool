import {ComponentFixture, TestBed} from '@angular/core/testing';

import {HomeComponent} from './home.component';
import {SearchService} from "../common/search.service";
import {of} from "rxjs";
import {Dictionary} from "../common/dictionary.model";
import {Filter} from "../common/filter.model";
import {ReactiveFormsModule} from "@angular/forms";
import {By} from "@angular/platform-browser";
import {FiltersEnum} from "../common/api.enum";
import {Component, DebugElement, Input} from "@angular/core";

describe('HomeComponent', () => {
  let component: HomeComponent;
  let fixture: ComponentFixture<HomeComponent>;
  let tagsFromApi: Dictionary;
  let getTagsSpy: any;

  @Component({selector: 'app-results', template: ''})
  class ResultsStubComponent {
    @Input() isLoadAllDatasets: Boolean;
    @Input() filterByTag: Filter;
    @Input() freeTextSearch: string;
  }

  @Component({selector: 'app-search', template: ''})
  class SearchStubComponent {
  }

  beforeEach(() => {
    tagsFromApi = new Dictionary({
      filters: {
        "tags": [
          new Filter({
            type: "tags",
            name: "tolstoy",
            display_name: "tolstoy",
            count: 1
          }),
          new Filter({
            type: "tags",
            name: "russian",
            display_name: "russian",
            count: 2
          }),
          new Filter({
            type: "tags",
            name: "medical",
            display_name: "medical",
            count: 1
          }),
          new Filter({
            type: "tags",
            name: "Flexible ",
            display_name: "Flexible ",
            count: 2
          })
        ]
      }
    });

    // spy for SearchService
    const spy = jasmine.createSpyObj('SearchService', ['getTags']);
    // Make the spy return a synchronous Observable with the test data
    getTagsSpy = spy.getTags.and.returnValue(of(tagsFromApi));

    TestBed.configureTestingModule({
      imports: [
        ReactiveFormsModule
      ],
      declarations: [
        HomeComponent,
        SearchStubComponent,
        ResultsStubComponent
      ],
      providers: [
        {provide: SearchService, useValue: spy}
      ]
    })
      .compileComponents(); // compile template and css

    fixture = TestBed.createComponent(HomeComponent);
    component = fixture.componentInstance;
    expect(component).toBeDefined();
  });

  afterEach(() => {
    component.reloadHomePage();
  });

  /**
   * retrieve top tags from remote api
   * show top tags on the screen as a button.
   */
  it('retrieve and show top tags', () => {

    const homeElement = fixture.nativeElement;
    let tagButtons: Array<HTMLButtonElement>;

    //test if component.topTags is empty
    expect(component.topTags).toEqual([]);
    // refresh the page
    fixture.detectChanges(); // onInit()
    //test if searchService returns expected top tags
    expect(getTagsSpy.calls.any()).toBe(true);
    //test if component is populates with expected value
    expect(component.topTags).toEqual(tagsFromApi.filters[FiltersEnum.tags]);

    // get all the top tag button from the view.
    tagButtons = homeElement.querySelectorAll('.btn-outline-info');
    // test the displayed top tags
    for (let i = 0; i < tagButtons.length; i++) {
      expect(
        tagButtons[i].textContent.toLowerCase().replace(/\s/g, "")
      ).toEqual(
        tagsFromApi.filters[FiltersEnum.tags][i].name.toLowerCase().replace(/\s/g, "")
      );
    }
  });

  /**
   * show result by applying tag filter
   */
  it('show result by applying tag filter', () => {
    const homeElement: HTMLElement = fixture.nativeElement;
    let tagButtons: NodeListOf<HTMLButtonElement>;

    // refresh the page
    fixture.detectChanges(); // onInit()
    //initial value must be null
    expect(component.filterByTag).toBe(null);
    // get first top tag button from the view.
    tagButtons = homeElement.querySelectorAll('.btn-outline-info');
    //click the button
    tagButtons[0].click();
    //refresh view
    fixture.detectChanges();

    //test the value of filter by tag must be updated.
    expect(component.filterByTag.isEqual(tagsFromApi.filters[FiltersEnum.tags][0])).toBe(true);
    expect(component.isLoadResults).toBe(true);
    expect(component.switchCase).toBe(1);
  });

  /**
   * show all datasets
   */
  it('show all datasets', () => {
    let dataSetsAnchorButton: DebugElement;

    // refresh the page
    fixture.detectChanges(); // onInit()
    //initial value must be false
    expect(component.isLoadAllDatasets).toBe(false);
    // get the reference to datasets button.
    dataSetsAnchorButton = fixture.debugElement.query(By.css('a[id=getAllDatasetsButton]'));
    //click the button
    dataSetsAnchorButton.triggerEventHandler('click', null);
    //refresh view
    fixture.detectChanges();
    //test the value of filter by tag must be updated.
    expect(component.isLoadAllDatasets).toBe(true);
    expect(component.isLoadResults).toBe(true);
    expect(component.switchCase).toBe(3);
  });

});
