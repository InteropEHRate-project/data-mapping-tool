import {Component, OnInit} from '@angular/core';
import {FormControl, FormGroup, Validators} from "@angular/forms";
import {RunProcessRequest} from "../common/request/runProcessRequest.model";
import {MatDialogRef} from "@angular/material/dialog";
import {STEPPER_GLOBAL_OPTIONS} from '@angular/cdk/stepper';
import {InputDataFormatEnum, InputSourceTypeEnum, OutputDataFormatEnum} from "../common/api.enum";

@Component({
  selector: 'app-create-process-dialog',
  templateUrl: './create-process-dialog.component.html',
  styleUrls: ['./create-process-dialog.component.css'],
  providers: [{
    provide: STEPPER_GLOBAL_OPTIONS, useValue: {showError: true}
  }]
})
export class CreateProcessDialogComponent implements OnInit {

  inputSourceType = InputSourceTypeEnum;
  outputDataFormat = OutputDataFormatEnum;
  inputDataFormat = InputDataFormatEnum;

  dataForm = new FormGroup({
    dataInputSource: new FormControl('', Validators.required),
    url: new FormControl('', Validators.required),
    file: new FormControl('', Validators.required)
  });

  modelForm = new FormGroup({
    modelInputSource: new FormControl('', Validators.required),
    url: new FormControl('', Validators.required),
    file: new FormControl('', Validators.required)
  });

  preferencesForm = new FormGroup({
    contentType: new FormControl('', Validators.required),
    outputDataFormat: new FormControl('', Validators.required),
  });

  constructor(
    public matDialogRef: MatDialogRef<CreateProcessDialogComponent>
  ) {
    matDialogRef.beforeClosed().subscribe(() => {
      if (this.dataForm.valid && this.modelForm.valid && this.preferencesForm.valid) {
        matDialogRef.close(new RunProcessRequest({
          datasetUrl: this.dataForm.controls['url'].value,
          dataFile: this.dataForm.controls['file'].value,
          r2rmlUrl: this.modelForm.controls['url'].value,
          r2rmlFile: this.modelForm.controls['file'].value,
          contentType: this.preferencesForm.controls['contentType'].value,
          outputDataFormat: this.preferencesForm.controls['outputDataFormat'].value
        }));
      } else {
        matDialogRef.close();
      }

    });
  }

  ngOnInit(): void {
    this.dataForm.controls['url'].disable();
    this.dataForm.controls['file'].disable();
    this.modelForm.controls['url'].disable();
    this.modelForm.controls['file'].disable();
  }

  onSubmit() {
    console.log("submit clicked");
    if (this.dataForm.valid && this.modelForm.valid && this.preferencesForm.valid) {
      this.matDialogRef.close();
    }
  }

  sourceTypeChanged(changedValue: InputSourceTypeEnum, formGroup: FormGroup) {
    if (formGroup) {
      if (changedValue === InputSourceTypeEnum.url) {
        formGroup.controls['url'].enable();
        formGroup.controls['file'].disable();
      } else if (changedValue === InputSourceTypeEnum.localFile) {
        formGroup.controls['url'].disable();
        formGroup.controls['file'].enable();
      }
    }
  }

  isControlEnabled(form: FormGroup, control: string) {
    return form.controls[control].enabled;
  }

  resetControlValue(form: FormGroup, control: string) {
    form.get(control)?.reset();
  }
}
