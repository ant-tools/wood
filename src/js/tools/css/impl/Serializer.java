package js.tools.css.impl;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import js.tools.commons.BugError;
import js.tools.css.CssException;
import js.tools.css.FontFaceRule;
import js.tools.css.ImportRule;
import js.tools.css.KeyFrame;
import js.tools.css.KeyFramesRule;
import js.tools.css.PropertyValue;
import js.tools.css.Ruleset;
import js.tools.css.StyleSheet;

final class Serializer
{
  private BufferedWriter writer;
  private boolean stdout;

  Serializer()
  {
  }

  Serializer(Writer writer)
  {
    this.writer = writer instanceof BufferedWriter ? (BufferedWriter)writer : new BufferedWriter(writer);
  }

  void serialize(StyleSheet styleSheet)
  {
    try {
      for(ImportRule importRule : styleSheet.getImportRules()) {
        serialize(importRule);
        writer.newLine();
      }
      if(!styleSheet.getImportRules().isEmpty()) {
        writer.newLine();
      }

      for(FontFaceRule fontFaceRule : styleSheet.getFontFaceRules()) {
        serialize(fontFaceRule);
        writer.newLine();
      }
      if(!styleSheet.getFontFaceRules().isEmpty()) {
        writer.newLine();
      }

      for(Ruleset ruleset : styleSheet.getRulesets()) {
        serialize(ruleset);
        writer.newLine();
        writer.newLine();
      }

      for(KeyFramesRule keyFramesRule : styleSheet.getKeyFramesRules()) {
        serialize(keyFramesRule);
        writer.newLine();
      }
    }
    catch(IOException e) {
      throw new CssException(e);
    }
    finally {
      try {
        if(this.stdout) {
          writer.flush();
        }
        else {
          writer.close();
        }
      }
      catch(IOException ignore) {}
    }
  }

  void minify(StyleSheet styleSheet)
  {
    throw new UnsupportedOperationException();
  }

  void dump(StyleSheet styleSheet)
  {
    try {
      writer = new BufferedWriter(new OutputStreamWriter(System.out, "UTF-8"));
    }
    catch(UnsupportedEncodingException e) {
      throw new BugError("JVM with missing support for UTF-8");
    }
    stdout = true;
    serialize(styleSheet);
  }

  private void serialize(ImportRule importRule) throws IOException
  {
    writer.write("@import url(\"");
    writer.write(importRule.getHref());
    writer.write("\");");
  }

  private void serialize(FontFaceRule fontFaceRule) throws IOException
  {
    writer.write("@font-face {");
    writer.newLine();
    writer.write("\tfont-family: \"");
    writer.write(fontFaceRule.getFontFamily());
    writer.write("\";");
    writer.newLine();
    writer.write("\tsrc: url(\"");
    writer.write(fontFaceRule.getSrc().toExternalForm());
    writer.write("\");");
    writer.newLine();
    writer.write('}');
  }

  private void serialize(KeyFramesRule keyFramesRule) throws IOException
  {
    writer.write("@keyframes ");
    writer.write(keyFramesRule.getAnimationName());
    writer.write(" {");
    writer.newLine();

    // current parser implementation does not allow an reasonable easy way to sort key frames
    // so we need to add this extra step
    // TODO refine parser
    SortedMap<Integer, List<PropertyValue>> keyFrames = new TreeMap<Integer, List<PropertyValue>>();
    for(KeyFrame keyFrame : keyFramesRule.getKeyFrames()) {
      for(Integer percent : keyFrame.getPercents()) {
        keyFrames.put(percent, keyFrame.getPropertyValues());
      }
    }

    for(Integer percent : keyFrames.keySet()) {
      writer.write("\t");
      writer.write(String.format("%d%%", percent));
      serialize(keyFrames.get(percent), 2);
      writer.newLine();
    }

    writer.write('}');
    writer.newLine();
  }

  private void serialize(Ruleset ruleset) throws IOException
  {
    serialize(ruleset.getSelectors());
    serialize(ruleset.getPropertyValues(), 1);
  }

  private void serialize(List<String> selectors) throws IOException
  {
    assert selectors.size() > 0;
    writer.write(selectors.get(0));
    for(int i = 1; i < selectors.size(); i++) {
      writer.write(',');
      writer.write(' ');
      writer.write(selectors.get(i));
    }
  }

  private void serialize(List<PropertyValue> propertyValues, int tabs) throws IOException
  {
    writer.write(' ');
    writer.write('{');
    writer.newLine();

    for(PropertyValue propertyValue : propertyValues) {
      for(int i = 0; i < tabs; ++i) {
        writer.write('\t');
      }
      serialize(propertyValue);
      writer.newLine();
    }

    for(int i = 1; i < tabs; ++i) {
      writer.write('\t');
    }
    writer.write('}');
  }

  private void serialize(PropertyValue propertyValue) throws IOException
  {
    writer.write(propertyValue.getProperty());
    writer.write(':');
    writer.write(' ');
    writer.write(propertyValue.getValue());
    writer.write(';');
  }
}
