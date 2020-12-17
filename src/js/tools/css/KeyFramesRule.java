package js.tools.css;

import java.util.List;

public interface KeyFramesRule
{
  String getAnimationName();

  List<KeyFrame> getKeyFrames();
}
