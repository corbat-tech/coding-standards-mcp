import {
  IsString,
  MinLength,
  MaxLength,
  IsArray,
  IsOptional,
  IsBoolean,
  ArrayMaxSize,
} from 'class-validator';

export class UpdateArticleDto {
  @IsString()
  @IsOptional()
  @MinLength(3)
  @MaxLength(200)
  title?: string;

  @IsString()
  @IsOptional()
  @MinLength(10)
  @MaxLength(50000)
  content?: string;

  @IsString()
  @IsOptional()
  @MinLength(2)
  @MaxLength(100)
  author?: string;

  @IsArray()
  @IsOptional()
  @IsString({ each: true })
  @ArrayMaxSize(10)
  tags?: string[];

  @IsBoolean()
  @IsOptional()
  published?: boolean;
}
