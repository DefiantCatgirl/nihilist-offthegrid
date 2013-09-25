NihilistView
============
(and later OffthegridView)

---

### As part of Lib.catgirl for Android ###

---

As my first foray into githubbing, here's a simple custom View for Android. Based roughly on the [Sony Developer custom ListView tutorial](http://developer.sonymobile.com/2010/05/20/android-tutorial-making-your-own-3d-list-part-1/) and borrowing some code from it, this custom ListView supports simple animated addition and deletion of the views.

It's not a drop-in for ListView, sadly, but it's fairly similar for simple purposes and supports recycling different view types with a simple adapter using a "tagged view" class instead of the traditional way. Personally I'm using it for a multi-level expandable list using another custom adapter in a yet-to-be-released app.

#### Things to be cautious about ####
...except for the horrible code in general (hey, I'm still learning):

* It's supposed to work on both 2.3+ and 3.0+, but I haven't been extensively testing the app for Gingerbread. There are some issues with animations on the pre-Honeycomb versions so if you notice or fix some things regarding that, please e-mail me or pull-request.

* If you're using NineOldAndroids, I advise you to use the other file, NihilistView_with_nineoldandroids.txt, instead of the NihilistView.java. It seems to work more smoothly than the standalone Animation.class-based version.

* If you're using NineOldAndroids for individual item animations and the normal NihilistView.java, make sure to reset all animation values on the disappearing views after their individual animation ends. Animation and Animator sometimes get out of sync and since the views are re-used, there's a chance the Animator will still run at least once after the list's own animation even if you make sure to use the same duration for both. It doesn't always happen, but it's one annoying issue I don't yet have a workaround for.

---

#### TO-DOs: ####
...sooner or later

* I will definitely provide a usage example later, most likely picking it out of the aforementioned app. I just wanted to do it with the next point:

* A tree adapter in the vein of [TreeViewList](https://code.google.com/p/tree-view-list-android/) to allow multi-level expandable menus. Pretty much an abstraction of what I'm already doing with this list.

* Some built-in thing to let the user pick when to 'select' the item or 'expand' the item (maybe?)

* The performance is far from stellar when deleting a lot of views.

* More options for the appearance-disappearance animations. Either an option for making the items themselves slide in or out, or providing a height of the appearing-disappearing block to the individual item animations so that they can use that to do whatever. Or both.

* OffthegridView, using the same common classes (and principles) - a grid view that supports animated addition, deletion and re-arranging of the views. I badly need it for the same app, so it's going to happen fairly soon.

* Backport rearranging of views from OffthegridView to NihilistView

---

Sorry for the wall of text. Any bugreports and/or pull requests are really welcome, I'd love to see this widget actually useful and usable, if anybody else actually needs it.

P.s. I'm still getting used to things over here, so sincerest apologies if I'm doing something wrong.