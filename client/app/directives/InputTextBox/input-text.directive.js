angular.module('wasdi.InputTextDirective', [])
    .directive('inputtextdirective', function () {
        "use strict";
        return {
            restrict: 'E',
            scope: {},

            // * Text binding ('@' or '@?') *
            // * One-way binding ('<' or '<?') *
            // * Two-way binding ('=' or '=?') *
            // * Function binding ('&' or '&?') *
            bindToController: {
                hero: '=',
                deleted: '&'
            },
    //         template: `
    //   <h2>{{$ctrl.hero.name}} details!</h2>
    //   <div><label>id: </label>{{$ctrl.hero.id}}</div>
    //   <button ng-click="$ctrl.onDelete()">Delete</button>
    // `,
            template: `
            <input type="text" class="form-control"  ng-model="$ctrl.hero">
         `,
            controller: function() {
                this.onDelete = () => {
                    this.deleted({hero: this.hero});
                };
            },
            controllerAs: '$ctrl'
        };
    });


