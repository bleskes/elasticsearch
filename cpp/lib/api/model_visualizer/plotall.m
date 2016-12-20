
function plotall (xax1,xax2,id)
  filename = ['./resultdir/',id];
  load(filename); # loads to variable 'A'
  i_filename = ['./inputdir/i_',id];
  source(i_filename); # loads to variable 'inIP'
  #set (1, 'defaultlinecolor', 'red');
  changepoint=3;
  iii=1;
  for x=[144:-1:1]    
    iii+=1;
    if iii > changepoint
      #axis([xlim1(1)-40 xlim1(2)+60 ylim1(1) ylim1(2)])
      axis([0 25 ylim1(1) ylim1(2)])
      yo = inIP(1:iii);
      yo_hi=max(yo);
      yo_lo=min(yo);
      interval_range = max(1,(yo_hi-yo_lo)/20);
      hist(inIP(1:iii),yo_lo:interval_range:yo_hi,1/interval_range,1, 'facecolor', 'r')
      hold on;
      #set (gca(), 'ylim', ylim1);
      #set (gca(), 'xlim', xlim1);
    elseif iii==changepoint
      xlim1 = get (gca (), 'xlim');
      xlimr = xlim1(2) - xlim1(1);
      perm_xlim = [xlim1(1)-0.1*xlimr, xlim1(2)+0.1*xlimr]
      ylim1 = get (gca (), 'ylim');
      ylimr = ylim1(2) - ylim1(1);
      perm_ylim = [ylim1(1)-0.1*ylimr, ylim1(2)+0.1*ylimr]
    endif
    plot(A{1,x},A{2,x}, 'linewidth',3)
    if iii> changepoint
      axis([xax1 xax2 0 1])
    endif
    hold off;
    input(['bucket: ', num2str(x)]);
  endfor
endfunction
