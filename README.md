[![](https://jitpack.io/v/ngtien137/Audio-Wave-View.svg)](https://jitpack.io/#ngtien137/Audio-Wave-View)
# Audio Wave View
Audio Sample View
## Preview 
![alt text](https://github.com/ngtien137/Audio-Wave-View/blob/master/preview/preview.gif) 
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
* Components:
![alt text](https://github.com/ngtien137/Audio-Wave-View/blob/master/preview/attributes.png) 
* All attributes
```xml
<declare-styleable name="AudioWaveView">
  <attr name="awv_zoom_able" format="boolean" />
  <attr name="awv_show_random_preview" format="boolean" />
  <attr name="awv_overlay_color_remove" format="color" />
  <attr name="awv_overlay_color_pick" format="color" />
  <attr name="awv_background_color" format="color" />
  <attr name="awv_bar_audio_height" format="dimension" />
  <attr name="awv_wave_bar_background_color" format="color" />

  <attr name="awv_wave_color" format="color" />
  <attr name="awv_wave_line_size" format="dimension" />
  <attr name="awv_wave_line_max_height" format="dimension" />
  <attr name="awv_wave_line_padding" format="dimension" />
  <attr name="awv_wave_zoom_min_level" format="float" />
  <attr name="awv_wave_zoom_max_level" format="float" />
  <attr name="awv_wave_zoom_level_auto" format="boolean" />

  <attr name="awv_text_timeline_color" format="color" />
  <attr name="awv_text_timeline_size" format="dimension" />
  <attr name="awv_text_timeline_padding_with_bar" format="dimension" />
  <attr name="awv_text_timeline_font" format="reference" />
  <attr name="awv_show_timeline_indicator" format="boolean" />
  <attr name="awv_indicator_timeline_color" format="color" />
  <attr name="awv_indicator_timeline_width" format="dimension" />
  <attr name="awv_indicator_timeline_height" format="dimension" />
  <attr name="awv_indicator_timeline_sub_indicator_count" format="integer" />
  <attr name="awv_indicator_timeline_sub_indicator_color" format="color" />

  <attr name="awv_thumb_progress_size" format="dimension" />
  <attr name="awv_thumb_progress_color" format="color" />
  <attr name="awv_thumb_progress_height" format="dimension" />
  <attr name="awv_thumb_progress_visible" format="boolean" />

  <attr name="awv_mode_edit">
      <enum name="none" value="0" />
      <enum name="trim" value="1" />
      <enum name="cut_out" value="2" />
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
  <attr name="awv_thumb_edit_circle_visible" format="boolean" />
  <attr name="awv_thumb_edit_circle_radius" format="dimension" />
  <attr name="awv_fixed_thumb_progress_by_thumb_edit" format="boolean" />
  <attr name="awv_thumb_progress_to_zero_after_initializing" format="boolean" />

  <attr name="awv_thumb_edit_text_value_size" format="dimension" />
  <attr name="awv_thumb_edit_text_value_color" format="color" />
  <attr name="awv_thumb_edit_text_value_font" format="reference" />
  <attr name="awv_thumb_edit_text_value_padding" format="dimension" />
  <attr name="awv_thumb_edit_text_value_pull_together" format="boolean" />
  <attr name="awv_thumb_min_space_between_text" format="dimension" />

  <attr name="awv_thumb_edit_text_value_position">
      <enum name="none" value="-1" />
      <enum name="bottom_of_anchor" value="0" />
      <enum name="top_of_anchor" value="1" />
  </attr>

  <attr name="awv_thumb_progress_mode">
      <enum name="flexible_mode" value="0" />
      <enum name="static_mode" value="1" />
  </attr>

  <attr name="awv_thumb_progress_static_position" format="enum|dimension">
      <enum name="left" value="-10" />
      <enum name="center" value="-11" />
  </attr>

  <attr name="awv_cache_mode" format="enum">
      <enum name="none" value="0" />
      <enum name="single" value="1" />
      <enum name="multiple" value="2" />
  </attr>

</declare-styleable>
```
* Callbacks and listeners
```java

audioWaveView.setInteractedListener(new AudioWaveView.IInteractedListener() {
  ...
});
audioWaveView.setAudioListener(new AudioWaveView.IAudioListener() {
  ...
});

public interface IAudioListener {
    void onLoadingAudio(int progress, boolean prepareView);

    void onLoadingAudioComplete();

    void onLoadingAudioError(Exception exceptionError);
}

public interface IInteractedListener {
    void onTouchDownAudioBar(float touchProgress, boolean touchInBar);

    void onClickAudioBar(float touchProgress, boolean touchInBar);

    void onTouchReleaseAudioBar(float touchProgress, boolean touchInBar);

    void onAudioBarScaling();

    void onRangerChanging(float minProgress, float maxProgress, AdjustMode adjustMode);

    void onStopFling(boolean isForcedStop);

    void onStartFling();

    void onProgressThumbChanging(float progress, ProgressAdjustMode progressAdjustMode);
}
```
