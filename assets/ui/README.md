# UI Artwork

`button_skin_atlas.png` is a 1 by 4 atlas. Rows are Normal, Selected, Disabled,
and Primary. Labels are drawn at runtime so one skin supports every menu and
action button. State artwork contains no decorative action symbols; selected
buttons are distinguished by their amber frame. The texture is loaded lazily
from presentation code only. Each row uses a slim frame and keeps the center
clear so runtime labels retain safe padding when rendered on small screens.
