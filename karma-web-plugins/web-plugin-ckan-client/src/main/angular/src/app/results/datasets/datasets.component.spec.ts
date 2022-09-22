import {async, ComponentFixture, TestBed} from '@angular/core/testing';

import {DatasetsComponent} from './datasets.component';
import {SearchService} from "../../common/search.service";
import {of} from "rxjs";
import {Dictionary} from "../../common/dictionary.model";
import {Filter} from "../../common/filter.model";

describe('DatasetsComponent', () => {
  let component: DatasetsComponent;
  let fixture: ComponentFixture<DatasetsComponent>;
  let responseFromApi: Dictionary;
  let getResponseSpy: any;

  beforeEach(async(() => {
    responseFromApi = new Dictionary({
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
    const spy = jasmine.createSpyObj('SearchService', ['getDataSetsAndFilters']);
    // Make the spy return a synchronous Observable with the test data
    getResponseSpy = spy.getDataSetsAndFilters.and.returnValue(of(responseFromApi));

    TestBed.configureTestingModule({
      declarations: [DatasetsComponent],
      providers: [
        {provide: SearchService, useValue: spy}
      ]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DatasetsComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeDefined();
  });
});
