# Audio Wave View
Audio Sample View
## Preview 
![alt text](https://github.com/ngtien137/Audio-Wave-View/blob/master/preview/preview.png) 
## Getting Started
### Configure build.gradle (Project)
* Add these lines:
```gradle
allprojects {
  repositories {
    ...
    maven { url 'https://jitpack.io' }
  }
}
```
### Configure build gradle (Module):
* Import module base:
```gradle
dependencies {
  implementation 'com.github.ngtien137:Audio-Wave-View:TAG'
}
```
* You can get version of this module [here](https://jitpack.io/#ngtien137/Audio-Wave-View)
## All Attributes 
```xml
  <declare-styleable name="AudioWaveView">
        <attr name="awv_show_random_preview" format="boolean" />
        <attr name="awv_overlay_color" format="color" />
        <attr name="awv_background_color" format="color" />
        <attr name="awv_bar_audio_height" format="dimension" />

        <attr name="awv_wave_color" format="color" />
        <attr name="awv_wave_line_size" format="dimension" />
        <attr name="awv_wave_line_max_height" format="dimension" />
        <attr name="awv_wave_line_padding" format="dimension" />

        <attr name="awv_text_timeline_color" format="color" />
        <attr name="awv_text_timeline_size" format="dimension" />
        <attr name="awv_text_timeline_padding_with_bar" format="dimension" />
        <attr name="awv_text_timeline_font" format="reference" />
        <attr name="awv_show_timeline_indicator" format="boolean" />
        <attr name="awv_indicator_timeline_color" format="color" />
        <attr name="awv_indicator_timeline_width" format="dimension" />
        <attr name="awv_indicator_timeline_height" format="dimension" />
        <attr name="awv_indicator_timeline_sub_indicator_count" format="integer" />
        <attr name="awv_indicator_timeline_sub_indicator_color" format="integer" />

        <attr name="awv_thumb_progress_size" format="dimension" />
        <attr name="awv_thumb_progress_color" format="color" />
        <attr name="awv_thumb_progress_height" format="dimension" />
        <attr name="awv_thumb_progress_visible" format="boolean" />

        <attr name="awv_mode_edit">
            <enum name="none" value="0" />
            <enum name="cut" value="1" />
            <enum name="trim" value="2" />
        </attr>

        <attr name="awv_progress" format="integer" />
        <attr name="awv_min_range_progress" format="integer" />
        <attr name="awv_max_progress" format="integer" />
        <attr name="awv_min_progress" format="integer" />
        <attr name="awv_duration" format="integer" />

        <attr name="awv_thumb_edit_background" format="color" />
        <attr name="awv_thumb_edit_visible" format="boolean" />
        <attr name="awv_thumb_edit_width" format="dimension" />
        <attr name="awv_thumb_edit_height" format="dimension" />
        <attr name="awv_thumb_edit_align">
            <enum name="top" value="0" />
            <enum name="bottom" value="1" />
            <enum name="center" value="2" />
        </attr>
        <attr name="awv_thumb_edit_left_anchor_align_horizontal">
            <enum name="center" value="2" />
            <enum name="left" value="3" />
            <enum name="right" value="4" />
        </attr>
        <attr name="awv_thumb_edit_left_anchor_align_vertical">
            <enum name="top" value="0" />
            <enum name="bottom" value="1" />
            <enum name="center" value="2" />
        </attr>
        <attr name="awv_thumb_edit_right_anchor_align_horizontal">
            <enum name="center" value="2" />
            <enum name="left" value="3" />
            <enum name="right" value="4" />
        </attr>
        <attr name="awv_thumb_edit_right_anchor_align_vertical">
            <enum name="top" value="0" />
            <enum name="bottom" value="1" />
            <enum name="center" value="2" />
        </attr>

        <attr name="awv_thumb_edit_left_anchor_image" format="reference" />
        <attr name="awv_thumb_edit_right_anchor_image" format="reference" />
        <attr name="awv_thumb_edit_anchor_width" format="dimension" />
        <attr name="awv_thumb_edit_anchor_height" format="dimension" />
        <attr name="awv_thumb_touch_expand_size" format="dimension" />
        <attr name="awv_fixed_thumb_progress_by_thumb_edit" format="boolean" />

    </declare-styleable>
```
