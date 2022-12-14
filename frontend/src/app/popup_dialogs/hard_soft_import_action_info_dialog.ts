// Copyright 2021 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

import {Component, ElementRef, Inject, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {FormBuilder, FormGroup, Validators} from '@angular/forms';
import {MatDialogRef} from '@angular/material/dialog';

/**
 * Snippet Action Info dialog component shows the information on
 * how to use SnippetValidationAction
 */
@Component({
  selector: 'hard_soft_import_action_info_dialog',
  templateUrl: 'hard_soft_import_action_info_dialog.ng.html',
  styleUrls: ['hard_soft_import_action_info_dialog.css']
})
export class HardAndSoftInfoDialogComponent {
  constructor(public dialogRef: MatDialogRef<HardAndSoftInfoDialogComponent>) {}

  onOkClick(): void {
    this.dialogRef.close();
  }
}
